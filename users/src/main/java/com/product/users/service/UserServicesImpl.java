package com.product.users.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.system.users.dto.UserRequest;
import com.system.users.dto.UserResponse;

import connection.ConnectionClass;

public class UserServicesImpl //implements IUserServices 
{

	//@Override
	public UserResponse createUser(UserRequest request) {

		UserResponse output = new UserResponse();

		String firstName = request.getFirstName();
		String lastName = request.getLastName();
		String emailId = request.getEmailId();
		String psd = request.getPsd();
		Integer errorCode=0;
		String errorDesc="";
		String userName="";

		try {
			//DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());

			try {
				Connection conn = null;
				conn = ConnectionClass.getEDBConnection();
				CallableStatement st = null;
				
				st=conn.prepareCall("call ot.pkg_user.createUser(?,?,?,?,?,?,?)");
				st.setString(1, firstName);
				st.setString(2, lastName);
				st.setString(3, emailId);
				st.setString(4, psd);
				st.registerOutParameter(5, java.sql.Types.VARCHAR);
				st.registerOutParameter(6, java.sql.Types.DOUBLE);
				st.registerOutParameter(7,java.sql.Types.VARCHAR);
				st.execute();
				
				userName=st.getString(5);
				errorCode=st.getInt(6);
				errorDesc=st.getString(7);
				

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		output.setErrorCode(errorCode);
		output.setErrorDesc(errorDesc);
		output.setUserName(userName);

		return output;

	}

}
