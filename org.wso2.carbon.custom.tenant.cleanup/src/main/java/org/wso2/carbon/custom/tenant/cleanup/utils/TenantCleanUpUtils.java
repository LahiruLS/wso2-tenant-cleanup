package org.wso2.carbon.custom.tenant.cleanup.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.custom.tenant.cleanup.exception.TenantCleanUpServiceException;
import org.wso2.carbon.custom.tenant.cleanup.internal.TenantCleanUpComponentServiceHolder;
import org.wso2.carbon.custom.tenant.cleanup.model.ServiceResponse;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

public class TenantCleanUpUtils {

    @SuppressWarnings("PackageAccessibility")
    private static final Log log = LogFactory.getLog(TenantCleanUpUtils.class);

    public static Connection getDatabaseConnection() throws SQLException, UserStoreException {

        RealmService realmService = TenantCleanUpComponentServiceHolder.getInstance().getRealmService();
        RealmConfiguration realmConfig = realmService.getBootstrapRealmConfiguration();
        if (realmConfig == null) {
            throw new UserStoreException("Error! Bootstrap realm config is null.");
        }
        DataSource dataSource = DatabaseUtil.getRealmDataSource(realmConfig);
        Connection dbConnection = DatabaseUtil.getDBConnection(dataSource);
        dbConnection.setAutoCommit(false);
        return dbConnection;
    }

    public static void closeAllConnections(Connection dbConnection, PreparedStatement... prepStmts) {

        closeStatements(prepStmts);
        closeConnection(dbConnection);
    }

    public static void closeAllConnections(Connection dbConnection, PreparedStatement prepStmts, ResultSet resultSet) {

        closeResultSets(resultSet);
        closeStatements(prepStmts);
        closeConnection(dbConnection);
    }

    private static void closeStatements(PreparedStatement... prepStmts) {

        if (prepStmts != null && prepStmts.length > 0) {
            for (PreparedStatement stmt : prepStmts) {
                closeStatement(stmt);
            }
        }
    }

    private static void closeResultSets(ResultSet... resultSets) {

        if (resultSets != null && resultSets.length > 0) {
            for (ResultSet rst : resultSets) {
                closeResultSet(rst);
            }
        }
    }

    private static void closeResultSet(ResultSet rs) {

        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Database error. Could not close result set  - " + e.getMessage(), e);
            }
        }
    }

    private static void closeConnection(Connection dbConnection) {

        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                log.error("Database error. Could not close statement. Continuing with others. - " + e.getMessage(), e);
            }
        }
    }

    private static void closeStatement(PreparedStatement preparedStatement) {

        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                log.error("Database error. Could not close statement. Continuing with others. - " + e.getMessage(), e);
            }
        }
    }

    public static void rollBack(Connection dbConnection) {

        try {
            if (dbConnection != null) {
                dbConnection.rollback();
            }
        } catch (SQLException e1) {
            log.error("An error occurred while rolling back transactions. ", e1);
        }
    }

    /**
     * Build success service response for a operation.
     *
     * @param scenario Scenario
     *                 {@link TenantCleanUpConstants.SuccessScenario}
     * @param data     Meta Data
     * @return response
     */
    public static ServiceResponse buildSuccessResponse(TenantCleanUpConstants.SuccessScenario scenario,
                                                       String... data) {

        String message = String.format(scenario.getMessage(), data);
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        ServiceResponse successResponse = new ServiceResponse();
        successResponse.setCode(scenario.getCode());
        successResponse.setMessage(message);
        return successResponse;
    }

    /**
     * Build failure service response for a operation.
     *
     * @param scenario Scenario
     *                 {@link TenantCleanUpConstants.FailureScenario}
     * @param data     Meta Data
     * @return response
     */
    public static ServiceResponse buildFailureResponse(TenantCleanUpConstants.FailureScenario scenario,
                                                       String... data) {

        String message = String.format(scenario.getMessage(), data);
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        ServiceResponse failureResponse = new ServiceResponse();
        failureResponse.setCode(scenario.getCode());
        failureResponse.setMessage(message);
        return failureResponse;
    }

    public static String getResponseInStringFormat(ServiceResponse response) {

        return StringUtils.join(new String[]{response.getCode(), response.getMessage()},
                TenantCleanUpConstants.SCENARIO_SEPARATOR);
    }

    public static String buildMarkedTenantDomain(String tenantDomain) {

        return StringUtils.join(new String[]{TenantCleanUpConstants.prefix, tenantDomain,
                Long.toString(System.currentTimeMillis())}, TenantCleanUpConstants.PREFIX_SEPARATOR);
    }

    public static void createTenantCleanUpScriptInDb() {

        //TODO:Need to compile the stored procedure during the bundle activation in an optional way.
    }

    /**
     * Validate the tenant domain
     *
     * @param domainName tenant domain
     * @throws TenantCleanUpServiceException , if invalid tenant domain name is given
     */
    public static void validateDomain(String domainName) throws TenantCleanUpServiceException {

        if (!StringUtils.isNotBlank(domainName)) {
            String msg = "Provided domain name is empty or null.";
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            throw new TenantCleanUpServiceException(msg);
        }
        if (StringUtils.indexOf(domainName, TenantCleanUpConstants.DOMAIN_SEPARATOR) < 1) {
            // can't start a domain starting with ".";
            String msg = "Invalid domain, starting with '.'";
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            throw new TenantCleanUpServiceException(msg);
        }
        // check the tenant domain contains any illegal characters
        if (domainName.matches(TenantCleanUpConstants.ILLEGAL_CHARACTERS_FOR_TENANT_DOMAIN)) {
            String msg = "The tenant domain ' " + domainName +
                    " ' contains one or more illegal characters. The valid characters are " +
                    "lowercase letters, numbers, '.', '-' and '_'.";
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            throw new TenantCleanUpServiceException(msg);
        }
        //check if the tenant is already marked for deletion
        if (StringUtils.startsWith(domainName, TenantCleanUpConstants.prefix)) {
            String msg =
                    "Illegal Attempt! Trying to retrieve tenant Id which already marked for deletion. Invalid tenant domain " +
                            "starting with: " + TenantCleanUpConstants.prefix;
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            throw new TenantCleanUpServiceException(msg);
        }
    }

    public static String buildCleanedUpTenantDomain(String tenantDomain) {

        return StringUtils.join(new String[]{TenantCleanUpConstants.prefix, tenantDomain, "%"},
                TenantCleanUpConstants.PREFIX_SEPARATOR);
    }

    public static void validateTenantId(int tenantId) throws TenantCleanUpServiceException {

        if (tenantId < 1) {
            String msg = "Invalid tenantId provided. tenantId: " + tenantId;
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            throw new TenantCleanUpServiceException(msg);
        }
    }

    public static String getTenantDomain(int tenantId) throws TenantCleanUpServiceException {

        validateTenantId(tenantId);
        try {
            return TenantCleanUpComponentServiceHolder.getTenantManager().getDomain(tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error occurred while retrieving the tenant domain for the ID: " + tenantId;
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            throw new TenantCleanUpServiceException(msg, e);
        }
    }
}
