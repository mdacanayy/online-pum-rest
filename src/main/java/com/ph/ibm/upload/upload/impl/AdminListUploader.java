/*
 *  Copyright (c) 2017 Nokia. All rights reserved.
 *
 *  Revision History:
 *
 *  DATE/AUTHOR          COMMENT
 *  ---------------------------------------------------------------------
 *  Nov 14, 2017/P100XX
 */
package com.ph.ibm.upload.upload.impl;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import com.ph.ibm.model.Employee;
import com.ph.ibm.model.Project;
import com.ph.ibm.model.ProjectEngagement;
import com.ph.ibm.opum.exception.InvalidCSVException;
import com.ph.ibm.opum.exception.InvalidEmployeeException;
import com.ph.ibm.repository.EmployeeRepository;
import com.ph.ibm.repository.ProjectEngagementRepository;
import com.ph.ibm.repository.ProjectRepository;
import com.ph.ibm.repository.impl.EmployeeRepositoryImpl;
import com.ph.ibm.repository.impl.ProjectEngagementRepositoryImpl;
import com.ph.ibm.repository.impl.ProjectRepositoryImpl;
import com.ph.ibm.upload.Uploader;
import com.ph.ibm.util.OpumConstants;
import com.ph.ibm.validation.Validator;
import com.ph.ibm.validation.impl.EmployeeValidator;

/**
 * Class implementation for uploading list of user administrator
 */
public class AdminListUploader implements Uploader {

    /**
     * EmployeeRepository is a Data Access Object which contain methods to add, register, login, view, validate field/s
     * stored in employee table - opum database
     */
    private EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();

    /**
     * ProjectRepository is a Data Access Object which contain method to retrieve fields stored in project table - opum
     * database
     */
    private ProjectRepository projectRepository = new ProjectRepositoryImpl();

    /**
     * ProjectEngagementRepository is a Data Access Object which contain method to add, save, get, check field/s stored
     * in project_engagement table - opum database
     */
    private ProjectEngagementRepository projectEngagementRepository = new ProjectEngagementRepositoryImpl();

    /**
     * Validation contain methods to validate field such as employee name, employee id, project name, email address
     */
    private Validator<Employee> validator = new EmployeeValidator(employeeRepository);

    /**
     * Logger is used to document the execution of the system and logs the corresponding log level such as INFO, WARN,
     * ERROR
     */
    private Logger logger = Logger.getLogger( AdminListUploader.class );

    /**
     * This method is used when Super Administrator uploads the list of Admin Users
     *
     * @param rawData Data from the CSV file
     * @param uriInfo uri information
     * @return @throws Exception exception
     * @see com.ph.ibm.upload.Uploader#upload(java.lang.String, javax.ws.rs.core.UriInfo)
     */
    @Override
    public Response upload( String rawData, UriInfo uriInfo ) throws Exception {

            ProjectEngagement projectEngagement = new ProjectEngagement();
            List<List<String>> rows = populateEmployeeProjectEngagements( rawData );
            List<Employee> validatedEmployee = new ArrayList<Employee>();
            Employee validateEmployee = new Employee();
            try{
                for( List<String> row : rows ){
                    validateEmployee = validateEmployee( uriInfo, row );
                    validatedEmployee.add( validateEmployee );
                    System.out.println( "Row Data: " + row );
                }
                for( Employee employee : validatedEmployee ){
                    Employee savedEmployee = employeeRepository.saveOrUpdate( employee );
                    if( savedEmployee != null ){
                        List<Project> projectdata = projectRepository.retrieveData();
                        for( Project project : projectdata ){
                            // if (!project.getProjectName().equals(validatedEmployee.get(3))) {
                            // TODO return invalidCsvResponseBuilder(uriInfo, employeeProjectEngagement,
                            // OpumConstants.INVALID_PROJECT_NAME);
                            // }

                            projectEngagement.setProjectId( project.getProjectId() );
                        }

                        projectEngagement.setEmployeeId( employeeRepository.viewEmployee( employee.getEmployeeSerial() ) );
                        projectEngagementRepository.addProjectEngagement( projectEngagement );
                    }
                }
            }
            catch( BatchUpdateException e ){
                logger.error( "BatchUpdateException due to " + e.getMessage() );
                System.out.println( e.getErrorCode() );
                return invalidCsvResponseBuilder( uriInfo, validateEmployee, OpumConstants.DUPLICATE_ENTRY );
                // }
            }
            catch( InvalidCSVException e ){
                logger.error( e.getError() );
                return invalidCsvResponseBuilder( uriInfo, e.getEmployee(), e.getError() );
            }
            catch( SQLException e ){
                logger.error( "SQL Exception due to " + e.getMessage() );
                e.printStackTrace();
                return Response.status( 206 ).header( "Location", uriInfo.getBaseUri() + "employee/" ).entity(
                    OpumConstants.SQL_ERROR ).build();
            }

            logger.info( OpumConstants.SUCCESSFULLY_UPLOADED_FILE );
            return Response.status( Status.OK ).header( "Location", uriInfo.getBaseUri() + "employee/" ).entity(
                "uploaded successfully" ).build();
        }

    /**
     * This method is used to generate error message for Upload List
     *
     * @param uriInfo uri information
     * @param e employee object
     * @param errorMessage error message
     * @return Response response
     */
    private Response invalidCsvResponseBuilder( UriInfo uriInfo, Employee e, String errorMessage ) {
        String invalidCsv;
        invalidCsv = String.format(
            "Invalid CSV for employee!\n\nSerial No: %s \nFull Name: %s \nIntranet Id: %s \nRoll In Date: %s \nRoll Off date: %s \n\nError message: %s",
            e.getEmployeeSerial(), e.getFullName(), e.getIntranetId(), e.getRollInDate(), e.getRollOffDate(),
            errorMessage, uriInfo );
        return Response.status( 206 ).header( "Location", uriInfo.getBaseUri() + "employee/" ).entity(
            invalidCsv ).build();
    }

    /**
     * This method is to populate list directly from CSV file
     *
     * @param rawData Data from the CSV file
     * @return Populated list of employee row data from CSV file
     */
    private List<List<String>> populateEmployeeProjectEngagements( String rawData ) {
        List<List<String>> employeeProjectEngagements = new ArrayList<List<String>>();
        List<String> row;
        String delimiter = ",";
        Scanner sc = new Scanner( rawData );
        ignoreFirstRow( sc );
        while( sc.hasNextLine() ){
            String line = sc.nextLine();
            if( !isRowEmpty( line ) && !line.startsWith( "----" ) ){
                row = Arrays.asList( line.split( delimiter ) );
                employeeProjectEngagements.add( row );
            }
        }
        sc.close();
        return employeeProjectEngagements;
    }

    /**
     * This method is used to validate uploaded list of Users/Employees
     *
     * @param uriInfo uri information
     * @param row represents row in csv file
     * @return Employee employee object
     * @throws Exception exception
     */
    private Employee validateEmployee( UriInfo uriInfo, List<String> row ) throws Exception {

        if( row == null || row.isEmpty() ){
            throw new InvalidEmployeeException( OpumConstants.INVALID_CSV );
        }
        Employee validateEmployee = new Employee();
        validateEmployee.setEmployeeSerial( row.get( 0 ) );
        validateEmployee.setFullName( row.get( 1 ) );
        validateEmployee.setIntranetId( row.get( 2 ) );
        validateEmployee.setRollInDate( row.get( 3 ) );
        validateEmployee.setRollOffDate( row.get( 4 ) );
        validator.validate( validateEmployee );
        return validateEmployee;
    }

    /**
     * This method used to ignore the header of the CSV file
     *
     * @param sc Scanner
     */
    private void ignoreFirstRow( Scanner sc ) {
        while( sc.hasNextLine() ){
            String line = sc.nextLine();
            if( isRowEmpty( line ) ){
                sc.nextLine();
                break;
            }
        }
    }

    /**
     * This method used to validate if CSV row data is Empty
     *
     * @param line represents row in csv file
     * @return boolean true if row is empty otherwise false
     */
    private boolean isRowEmpty( String line ) {
        return line == null || line.equals( "\\n" ) || line.equals( "" );
    }

}
