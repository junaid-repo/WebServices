package com.jdbcdemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdbcdemo.jdbcdemo.Services;
import com.jdbcdemo.jdbcdemo.coreCall.CoreServiceCall;
import com.jdbcdemo.jdbcdemo.dto.BaseOutput;
import com.jdbcdemo.jdbcdemo.dto.BulkEmployeesResponse;
import com.jdbcdemo.jdbcdemo.dto.CountryGDPResponse;
import com.jdbcdemo.jdbcdemo.dto.DepartmentDetailsResponse;
import com.jdbcdemo.jdbcdemo.dto.EmailRequest;
import com.jdbcdemo.jdbcdemo.dto.EmployeeBulkDeleteRequest;
import com.jdbcdemo.jdbcdemo.dto.EmployeeDetailsRequest;
import com.jdbcdemo.jdbcdemo.dto.EmployeeDetailsResponse;
import com.jdbcdemo.jdbcdemo.dto.InsertEmployeeList;
import com.jdbcdemo.jdbcdemo.dto.InsertEmployeeRequest;
import com.jdbcdemo.jdbcdemo.dto.InsertEmployeeResponse;
import com.jdbcdemo.jdbcdemo.dto.InsertEmployeeResponse2;
import com.jdbcdemo.jdbcdemo.dto.JobDetails;
import com.jdbcdemo.jdbcdemo.dto.SalaryOperationsResponse;
import com.jdbcdemo.jdbcdemo.dto.TranslateText;
import com.jdbcdemo.jdbcdemo.interfaces.IDownloadFile;
import com.jdbcdemo.jdbcdemo.interfaces.IExportTableDataAsScript;
import com.jdbcdemo.jdbcdemo.interfaces.IFN02;
import com.jdbcdemo.jdbcdemo.interfaces.IFN03;
import com.jdbcdemo.jdbcdemo.interfaces.IGDPCountries;
import com.jdbcdemo.jdbcdemo.interfaces.ISendSimpleEmail;
import com.jdbcdemo.jdbcdemo.interfaces.IServices;
import com.jdbcdemo.jdbcdemo.interfaces.ITextTranslate;
import com.jdbcdemo.jdbcdemo.interfaces.IUploadFile;
import com.jdbcdemo.security.AuthenticationRequest;
import com.jdbcdemo.security.AuthenticationResponse;
import com.jdbcdemo.security.JwtUtil;
import com.jdbcdemo.security.MyUserDetailsService;

import excelProject.CSVReader;
import excelProject.ImportURL;

@RestController
public class SimpleController {
	@Autowired
	IServices serv;

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	MyUserDetailsService userDetailsService;

	@Autowired
	JwtUtil jwtTokenUtil;

	private static String FILE_DIRECTORY = "C:/Users/junai/OneDrive/Documents/FileUploadDir/";

	@RequestMapping(value = "webService/jdbcDemo", method = RequestMethod.GET)
	ResponseEntity<BaseOutput> simpleControllerDemo() {

		BaseOutput bs = new BaseOutput();
		bs.setErrorCode(133);
		bs.setErrorDesc("Success");
		return new ResponseEntity<BaseOutput>(bs, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/getEmployeeLists", method = RequestMethod.GET)
	ResponseEntity<EmployeeDetailsResponse> getEmpDetails(@RequestBody EmployeeDetailsRequest empOb) {

		// System.out.println(empId);
		String empId = empOb.getDeptId();

		EmployeeDetailsResponse output = new EmployeeDetailsResponse();
		if (empId == null || empId == "") {

			output.setErrorDesc(HttpStatus.BAD_REQUEST.getReasonPhrase());
			output.setErrorCode(HttpStatus.BAD_REQUEST.value());
			return new ResponseEntity<EmployeeDetailsResponse>(output, HttpStatus.BAD_REQUEST);
		}
		// IServices serv = new Services();
		try {
			output = serv.getEmployeeLists(empId);
		} catch (Exception e) {
			output.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			output.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return new ResponseEntity<EmployeeDetailsResponse>(output, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		output.setErrorDesc(HttpStatus.OK.getReasonPhrase());
		output.setErrorCode(HttpStatus.OK.value());

		return new ResponseEntity<EmployeeDetailsResponse>(output, HttpStatus.OK);

	}

	@RequestMapping(value = "webService/addEmployee", method = RequestMethod.POST)
	ResponseEntity<InsertEmployeeResponse> insertNewEmployee(@RequestBody InsertEmployeeList request) {

		InsertEmployeeResponse response = new InsertEmployeeResponse();

		try {
			response = serv.insertNewEmployee(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			response.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			// return new ResponseEntity<InsertEmployeeResponse>(response,
			// HttpStatus.INTERNAL_SERVER_ERROR);
		}

		String location = ServletUriComponentsBuilder
				.fromHttpUrl(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() + "/getEmployeeDetails")
				.path("/{id}").buildAndExpand(response.getEmpId()).toUri().toString();

		System.out.println(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());

		response.setErrorDesc(HttpStatus.CREATED.getReasonPhrase());
		response.setErrorCode(HttpStatus.CREATED.value());
		response.setEmpId(response.getEmpId());
		response.setUrl(location);
		// return ResponseEntity.created(location).build();
		return new ResponseEntity<InsertEmployeeResponse>(response, HttpStatus.OK);

	}

	@RequestMapping(value = "webService/getEmployeeDetails/{empId}", method = RequestMethod.GET)
	@Produces(MediaType.APPLICATION_XML)
	ResponseEntity<EmployeeDetailsResponse> getEmployeeDetails(@PathVariable String empId) {
		EmployeeDetailsResponse response = new EmployeeDetailsResponse();
		System.out.println("yahan tak pauch gaye ");
		if (empId == null || empId == "") {

			response.setErrorDesc(HttpStatus.BAD_REQUEST.getReasonPhrase());
			response.setErrorCode(HttpStatus.BAD_REQUEST.value());
			return new ResponseEntity<EmployeeDetailsResponse>(response, HttpStatus.BAD_REQUEST);
		}
		try {
			response = serv.getEmployeeDetails(empId);
			if (response.getName() == "" || response.getName() == null) {
				response.setErrorDesc(HttpStatus.NOT_FOUND.getReasonPhrase());
				response.setErrorCode(HttpStatus.NOT_FOUND.value());
				return new ResponseEntity<EmployeeDetailsResponse>(response, HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			response.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return new ResponseEntity<EmployeeDetailsResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		/*
		 * response.setErrorDesc(HttpStatus.OK.getReasonPhrase());
		 * response.setErrorCode(HttpStatus.OK.value());
		 */

		return new ResponseEntity<EmployeeDetailsResponse>(response, HttpStatus.OK);

	}

	@RequestMapping(value = "webService/deleteUser/{empId}", method = RequestMethod.DELETE)
	ResponseEntity<BaseOutput> removeEmployee(@PathVariable String empId) {
		BaseOutput response = new BaseOutput();

		try {
			response = serv.removeEmployee(empId);
		} catch (Exception e) {
			e.printStackTrace();
			response.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			response.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			System.out.println("ar");
			return new ResponseEntity<BaseOutput>(response, HttpStatus.INTERNAL_SERVER_ERROR);

		}
		response.setErrorDesc(HttpStatus.OK.getReasonPhrase());
		response.setErrorCode(HttpStatus.OK.value());

		return new ResponseEntity<BaseOutput>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/freezeEmployee/{empId}", method = RequestMethod.POST)
	ResponseEntity<BaseOutput> freezeEmployee(@PathVariable String empId) {
		BaseOutput response = new BaseOutput();
		Services serv = new Services();
		response = serv.freezeEmployee(empId);

		return new ResponseEntity<BaseOutput>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/createBulkEmployee", method = RequestMethod.POST)
	ResponseEntity<BulkEmployeesResponse> createBulkEmployee(@RequestBody InsertEmployeeRequest employeeList) {
		// BaseOutput response = new BaseOutput();
		BulkEmployeesResponse response = new BulkEmployeesResponse();
		Services serv = new Services();
		try {
			response = serv.createBulkEmployee(employeeList.getEmployeeList());
		} catch (Exception e) {
			response.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			response.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			e.printStackTrace();
		}

		List<InsertEmployeeResponse> newEmpRespnse = new ArrayList<>();
		newEmpRespnse = response.getNewEmployeesResponse();
		List<InsertEmployeeResponse> newEmployeesResponse = new ArrayList<>();

		for (InsertEmployeeResponse ier : newEmpRespnse) {
			String location = ServletUriComponentsBuilder
					.fromHttpUrl(
							ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() + "/getEmployeeDetails")
					.path("/{id}").buildAndExpand(ier.getEmpId()).toUri().toString();

			System.out.println(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());
			InsertEmployeeResponse obj = new InsertEmployeeResponse();

			obj.setErrorDesc(HttpStatus.CREATED.getReasonPhrase());
			obj.setErrorCode(HttpStatus.CREATED.value());
			obj.setEmpId(ier.getEmpId());
			obj.setUrl(location);
			newEmployeesResponse.add(obj);
		}
		response.setNewEmployeesResponse(newEmployeesResponse);
		// return ResponseEntity.created(location).build();

		return new ResponseEntity<BulkEmployeesResponse>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/importBulkUsers", method = RequestMethod.POST)
	public ResponseEntity<BulkEmployeesResponse> importUsers(@RequestBody ImportURL fileLocation) {
		// BaseOutput response = new BaseOutput();
		String loc = "C:\\Users\\junai\\Downloads\\BulkEmployees.csv";
		CSVReader rd = new CSVReader();
		String jsonString = "";
		try {
			jsonString = rd.csvRead(fileLocation.getFileLocation());
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		ObjectMapper mapper = new ObjectMapper();
		List<InsertEmployeeList> arList = new ArrayList<>();
		try {
			arList = mapper.readValue(jsonString, new TypeReference<List<InsertEmployeeList>>() {
			});
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		BulkEmployeesResponse response = new BulkEmployeesResponse();
		Services serv = new Services();
		try {
			response = serv.createBulkEmployee(arList);
		} catch (Exception e) {
			response.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			response.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			e.printStackTrace();
		}

		List<InsertEmployeeResponse> newEmpRespnse = new ArrayList<>();
		newEmpRespnse = response.getNewEmployeesResponse();
		List<InsertEmployeeResponse> newEmployeesResponse = new ArrayList<>();

		for (InsertEmployeeResponse ier : newEmpRespnse) {
			String location = ServletUriComponentsBuilder
					.fromHttpUrl(
							ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() + "/getEmployeeDetails")
					.path("/{id}").buildAndExpand(ier.getEmpId()).toUri().toString();

			System.out.println(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());
			InsertEmployeeResponse obj = new InsertEmployeeResponse();

			obj.setErrorDesc(HttpStatus.CREATED.getReasonPhrase());
			obj.setErrorCode(HttpStatus.CREATED.value());
			obj.setEmpId(ier.getEmpId());
			obj.setUrl(location);
			newEmployeesResponse.add(obj);
		}
		response.setNewEmployeesResponse(newEmployeesResponse);
		// return ResponseEntity.created(location).build();

		return new ResponseEntity<BulkEmployeesResponse>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/deleteBulkUser", method = RequestMethod.POST)
	ResponseEntity<BaseOutput> removeBulkEmployee(@RequestBody ImportURL fileLocation) {
		BaseOutput response = new BaseOutput();
		CSVReader rd = new CSVReader();
		String jsonString = "";
		try {
			jsonString = rd.csvRead(fileLocation.getFileLocation());
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		ObjectMapper mapper = new ObjectMapper();
		List<EmployeeBulkDeleteRequest> arList = new ArrayList<>();
		try {
			arList = mapper.readValue(jsonString, new TypeReference<List<EmployeeBulkDeleteRequest>>() {
			});
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			for (EmployeeBulkDeleteRequest empList : arList) {
				response = serv.removeEmployee(empList.getEmpId());
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			response.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return new ResponseEntity<BaseOutput>(response, HttpStatus.INTERNAL_SERVER_ERROR);

		}
		response.setErrorDesc(HttpStatus.OK.getReasonPhrase());
		response.setErrorCode(HttpStatus.OK.value());

		return new ResponseEntity<BaseOutput>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/deptWiseTotalSalary/{type}/{deptId}", method = RequestMethod.GET)
	ResponseEntity<Double> deptWiseTotalSalary(@PathVariable String type, @PathVariable String deptId) {

		System.out.println(type);
		System.out.println(deptId);
		Double sum = 0D;
		sum = serv.sumOfDeptWiseSalary(deptId);

		return new ResponseEntity<Double>(sum, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/wiseCalculation/{wise}/{type}/{id}", method = RequestMethod.GET)
	ResponseEntity<SalaryOperationsResponse> wiseCalculation(@PathVariable String wise, @PathVariable String type,
			@PathVariable String id) {
		SalaryOperationsResponse response = new SalaryOperationsResponse();
		System.out.println(wise);
		System.out.println(type);
		System.out.println(id);
		Double amount = 0D;
		try {
			amount = serv.wiseCalcuation(wise, type, id);

			response.setErrorDesc(HttpStatus.OK.getReasonPhrase());
			response.setErrorCode(HttpStatus.OK.value());

		} catch (Exception e) {
			response.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			response.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			e.printStackTrace();

		}

		response.setOperation(type);
		response.setAmount(amount);

		return new ResponseEntity<SalaryOperationsResponse>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/jobDetailsWithMinimumSalaryOfAndSortedByMaxSalaryDesc/{minimumSalary}", method = RequestMethod.GET)
	ResponseEntity<List<JobDetails>> jobDetails(@PathVariable String minimumSalary) {

		List<JobDetails> response = new ArrayList<>();
		try {
			response = serv.jobDetails(minimumSalary);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResponseEntity<List<JobDetails>>(response, HttpStatus.OK);

	}

	@RequestMapping(value = "webService/getEmpIdAccordingToMiniumSalaryOf/{minSalary}", method = RequestMethod.GET)
	ResponseEntity<List<InsertEmployeeResponse2>> getEmpId(@PathVariable String minSalary) {
		List<InsertEmployeeResponse2> response = new ArrayList<>();
		CoreServiceCall csc = new CoreServiceCall();

		List<Integer> empList = new ArrayList<>();
		IFN02 fn = new Services();
		try {
			empList = fn.empIdListNew(minSalary);
		} catch (Exception e) {
			e.printStackTrace();
			InsertEmployeeResponse obj1 = new InsertEmployeeResponse();
			obj1.setErrorDesc(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
			obj1.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			// return new ResponseEntity<InsertEmployeeResponse>(response,
			// HttpStatus.INTERNAL_SERVER_ERROR);
		}
		for (Integer empI : empList) {
			String loc = ServletUriComponentsBuilder
					.fromHttpUrl(
							ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() + "/getEmployeeDetails")
					.path("/{id}").buildAndExpand(empI).toUriString();

			System.out.println(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());
			InsertEmployeeResponse2 obj = new InsertEmployeeResponse2();
			Double salary = 0D;

			salary = csc.getEmpSalary(empI.toString());
			obj.setErrorDesc(HttpStatus.CREATED.getReasonPhrase());
			obj.setErrorCode(HttpStatus.CREATED.value());
			obj.setEmpId(empI.toString());
			obj.setSalary(salary);

			obj.setUrl(loc);
			response.add(obj);

		}
		// return ResponseEntity.created(location).build();
		return new ResponseEntity<List<InsertEmployeeResponse2>>(response, HttpStatus.OK);

	}

	@RequestMapping(value = "webService/getDetpartmentDetails/{deptId}", method = RequestMethod.GET)
	ResponseEntity<DepartmentDetailsResponse> getDepartmentDetails(@PathVariable String deptId,
			@RequestParam String name, String name2) {
		DepartmentDetailsResponse response = new DepartmentDetailsResponse();
		IFN03 obj = new Services();
		System.out.println("the input taken in the request are ------------------->" + name);
		System.out.println("the input taken in the request are -------------------2>" + name2);

		response = obj.getDeptDetails(deptId);

		return new ResponseEntity<DepartmentDetailsResponse>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/createAndUploadTablesAndData", method = RequestMethod.POST)
	ResponseEntity<BaseOutput> createAndUploadTablesAndData(@RequestBody ImportURL fileLocation) {
		BaseOutput output = new BaseOutput();

		try {
			output = serv.tableAndDataCreate(fileLocation.getFileLocation());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<BaseOutput>(output, HttpStatus.OK);
	}

	@RequestMapping(value = "webService/exportTableDataAs_SQL_Script/{tableName}", method = RequestMethod.POST)
	ResponseEntity<BaseOutput> exportTableDataAsDBScript(@PathVariable String tableName) {
		BaseOutput response = new BaseOutput();

		IExportTableDataAsScript obj = new Services();

		try {
			response = obj.exportTableDataAsScript(tableName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<BaseOutput>(response, HttpStatus.CREATED);
	}

	@RequestMapping(value = "webService/getGPDWiseCountries", method = RequestMethod.GET)
	ResponseEntity<CountryGDPResponse> gpdWiseCountries(@RequestParam String gpd, String year) {
		CountryGDPResponse response = new CountryGDPResponse();

		AuthenticationRequest auth = new AuthenticationRequest();
		// AuthenticationResponse authRes = new AuthenticationResponse(null);
		auth.setUserName("dev");
		auth.setPassword("dev");

		try {
			createAuthenticationToken(auth);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		System.out.println(gpd);
		System.out.println(year);

		IGDPCountries obj = new Services();

		try {
			response = obj.getGDPWiseCounrties(year, gpd);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<CountryGDPResponse>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/authenticate", method = RequestMethod.POST)
	ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticateRequest)
			throws Exception {

		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authenticateRequest.getUserName(), authenticateRequest.getPassword()));
		} catch (AuthenticationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticateRequest.getUserName());

		final String jwt = jwtTokenUtil.generateToken(userDetails);
		System.out.println(jwt);

		return ResponseEntity.ok(new AuthenticationResponse(jwt));
	}

	@RequestMapping(value = "webService/uploadDataTable", method = RequestMethod.POST)
	ResponseEntity<BaseOutput> createAndUploadTablesAndDataWithFileUpload(@RequestParam("File") MultipartFile file) {
		BaseOutput output = new BaseOutput();
		String response = null;
		File myFile = null;
		String fileLocation = "";
		CoreServiceCall core = new CoreServiceCall();
		try {
			IUploadFile upload = new Services();
			response = upload.uploadFile(file);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (!response.equals(null)) {

			try {
				fileLocation = core.getDocLocationWithDocId(response);
				if (fileLocation != "") {
					output = serv.tableAndDataCreate(fileLocation);
				}
				/*
				 * if (output.getErrorCode() == 0) { // File deleteFile = new File(response);
				 * 
				 * Files.deleteIfExists(Paths.get(response)); }
				 */

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new ResponseEntity<BaseOutput>(output, HttpStatus.OK);
	}

	@RequestMapping(value = "/fileUpload", method = RequestMethod.POST)
	public ResponseEntity<Object> fileUpload(@RequestParam("File") MultipartFile file) throws IOException {

		// String fileLocation =
		// "C:\\Users\\junai\\OneDrive\\Documents\\FileUploadDir\\";
		IUploadFile upload = new Services();

		String response = upload.uploadFile(file);

		String location = ServletUriComponentsBuilder
				.fromHttpUrl(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() + "/fileDownload")
				.path("/{id}").buildAndExpand(response).toUri().toString();

		System.out.println(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());
		return new ResponseEntity<Object>(location, HttpStatus.OK);
	}

	@RequestMapping(value = "tableDataAs_SQL_Script/{tableName}", method = RequestMethod.POST)
	ResponseEntity<BaseOutput> exportTableDataAsDBScriptWithThreading(@PathVariable String tableName) {
		BaseOutput response = new BaseOutput();

		Services obj = new Services();

		try {
			response = obj.exportTableDataAsScriptWithThreads(tableName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<BaseOutput>(response, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/fileDownload/{docId}", method = RequestMethod.GET)
	public ResponseEntity<Resource> fileDownload(@PathVariable String docId) throws IOException {

		MultiPart objMultiPart = null;
		String filePath = "";
		File file = null;
		// String fileName = "";
		ByteArrayResource resource = null;
		HttpHeaders header = null;

		try {
			IDownloadFile down = new Services();
			filePath = down.DownloadFile(docId);
			file = new File(filePath);

			resource = new ByteArrayResource(
					java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(file.getAbsolutePath())));
			header = new HttpHeaders();
			header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ResponseEntity.ok().headers(header).contentLength(file.length())
				.contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
				.body(resource);

	}

	@RequestMapping(value = "/external/translateText", method = RequestMethod.POST)
	public ResponseEntity<String> translateText(@RequestBody TranslateText textToTranslate) {

		ITextTranslate obj = new Services();
		String translatedText = "";

		translatedText = obj.textTranslate(textToTranslate);

		return new ResponseEntity<String>(translatedText, HttpStatus.OK);

	}

	@RequestMapping(value = "/external/sendSimpleEmail", method = RequestMethod.POST)
	public ResponseEntity<BaseOutput> sendSimpleEmail(@RequestBody EmailRequest emailRequest) {

		BaseOutput response = new BaseOutput();
		ISendSimpleEmail email = new Services();

		try {
			response = email.sendSimpleMail(emailRequest);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<BaseOutput>(response, HttpStatus.OK);
	}

}
