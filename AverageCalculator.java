import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;


public class AverageCalculator {

    



    private static final String APPLICATION_NAME = "Weekly Average Calculator";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    
    private static final List<String> SCOPES = Arrays.asList(
        SheetsScopes.SPREADSHEETS
    );

    
    
    
    

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";


    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException{
        InputStream in = AverageCalculator.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null)
            throw new FileNotFoundException("Wasn't found gang " + CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver reciever = new LocalServerReceiver.Builder().setPort(8080).build();
        return new AuthorizationCodeInstalledApp(flow, reciever).authorize("user");
    }

    public String averageInputter(String dataSheetID, String newSpreadsheetID, String Range) throws IOException, GeneralSecurityException{
        final NetHttpTransport HTTP_Transport = GoogleNetHttpTransport.newTrustedTransport();
        //TO DO - Find Spreadsheet ID and add it in
        final String spreadsheetID = dataSheetID;
        //To Do - Add in the range
        final String range = Range;

        Sheets service = new Sheets.Builder(HTTP_Transport, JSON_FACTORY, getCredentials(HTTP_Transport))
                .setApplicationName(APPLICATION_NAME)
                .build();
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetID, range)
                .execute();

        List<List<Object>> values = response.getValues();
        
        String newSpreadSheetID = newSpreadsheetID;
        clearSpreadsheet(newSpreadSheetID, service);
        


        
        if(values==null || values.isEmpty()) {
            return "No data gang";
        } else {
            System.out.println("Date, Weight");
            int i = 1; // Start at 1 to align with spreadsheet rows
            double weeklyAverage = 0;
            String startDate = "";
            for(List<Object> row: values) {
                weeklyAverage += Double.parseDouble(row.get(1).toString());
                if(i % 7 == 1) { // Start of week
                    startDate = (String) row.get(0);
                }
                if(i % 7 == 0) { // End of week
                    String endDate = (String) row.get(0);
                    String dateRange = startDate + " - " + endDate;
                    weeklyAverage = weeklyAverage / 7.0;
                    
                    // Add to new spreadsheet (using actual row number)
                    int rowNum = (i / 7) + 1; // Calculate correct row number
                    addRow(newSpreadSheetID, "A" + rowNum + ":B" + rowNum, 
                          Arrays.asList(dateRange, weeklyAverage), service);
                    
                    weeklyAverage = 0; // Reset for next week
                }
                i++;
            }
            return "Success";
        }
    }

    

    //need to add a test method that basically tests how the data is being added wihtout adding it to the spreadsheet

    public static void testSpreadsheet(String spreadsheetID, Sheets service) throws IOException{
        addRow(spreadsheetID, "A2:B2", Arrays.asList("Test", "Test"), service);
    }

    public static void clearSpreadsheet(String spreadsheetID, Sheets service) throws IOException{
        ClearValuesRequest request = new ClearValuesRequest();
        service.spreadsheets().values().clear(spreadsheetID, "A1:Z1000", request).execute();
    }
    
    public static void setSpreadsheet(String spreadsheetID, Sheets service) throws IOException{
        clearSpreadsheet(spreadsheetID, service);
        ValueRange body = new ValueRange().setValues(Collections.singletonList(Arrays.asList("Weekly Average")));

        service.spreadsheets().values().update(spreadsheetID, "A1", body)
            .setValueInputOption("USER_ENTERED")
            .execute();
    }
    public static String createSpreadsheet(String title, Sheets service) throws IOException {
        
    // Create new spreadsheet with a title
    Spreadsheet spreadsheet = new Spreadsheet()
        .setProperties(new SpreadsheetProperties()
            .setTitle(title));
    spreadsheet = service.spreadsheets().create(spreadsheet)
        .setFields("spreadsheetId")
        .execute();
        
    // Prints the new spreadsheet id
    System.out.println("Spreadsheet ID: " + spreadsheet.getSpreadsheetId());
    return spreadsheet.getSpreadsheetId();
  }

  public static void addRow(String spreadsheetID, String range, List<Object> rowData, Sheets service) throws IOException{
    ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));

    service.spreadsheets().values().append(spreadsheetID, range, body)
        .setValueInputOption("USER_ENTERED")
        .setInsertDataOption("INSERT_ROWS")
        .execute();
    System.out.println("Row added successfully");
}


}
