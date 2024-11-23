import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
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
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.Document;
import java.util.Objects;
import java.util.Arrays;


public class Main {



    private static final String APPLICATION_NAME = "Docs to Google Sheets";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final String DOCUMENT_ID = "1W1UF_RDkzf5L_xyXJbLf5Q0ml2lykowVLB0G5KjXITk";
    private static final List<String> SCOPES = Arrays.asList(
        SheetsScopes.SPREADSHEETS,
        DocsScopes.DOCUMENTS_READONLY
    );

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";


    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException{
        InputStream in = Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null)
            throw new FileNotFoundException("Shit wasn't found gang " + CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver reciever = new LocalServerReceiver.Builder().setPort(8080).build();
        return new AuthorizationCodeInstalledApp(flow, reciever).authorize("user");
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException{
        final NetHttpTransport HTTP_Transport = GoogleNetHttpTransport.newTrustedTransport();
        //TO DO - Find Spreadsheet ID and add it in
        final String spreadsheetID = "1OYMYgFiRsiQgzT9SixmL_CyqPFY7w6uLurMzqvAbsIE";
        //To Do - Add in the range
        final String range = "A2:B325";

        Sheets service = new Sheets.Builder(HTTP_Transport, JSON_FACTORY, getCredentials(HTTP_Transport))
                .setApplicationName(APPLICATION_NAME)
                .build();
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetID, range)
                .execute();

        List<List<Object>> values = response.getValues();

        if(values==null || values.isEmpty())
            System.out.println("No data gang");
        else{
            System.out.println("Date, Weight");
            int i = 0;
            double weeklyAverage = 0;
            String newSpreadSheetID = createSpreadsheet("Weekly Average", service);
            System.out.println("New Spreadsheet created :" + newSpreadSheetID);
            String startDate = "";
            for(List<Object> row: values){
                System.out.printf("%s, %s\n", row.get(0), row.get(1));
                weeklyAverage += Double.parseDouble(row.get(1).toString());
                if(i==0)
                    startDate = (String) row.get(0);
                if(i%7 == 0 && i != 0){
                    startDate += " - " + row.get(0);
                    weeklyAverage/=7.0;
                    System.out.println("Weekly average: " + weeklyAverage);
                    List<Object> rowData = Arrays.asList(startDate, weeklyAverage);
                    addRow(newSpreadSheetID, "A" + i+2 +":B" +i+2, rowData, service);
                    
                    weeklyAverage = 0;
                    startDate = "";
                }
                i++;
            }
        }
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

  public static void addRow(String spreadsheetID, String range, List<Object> row, Sheets service) throws IOException{
    ValueRange body = new ValueRange().setValues(Collections.singletonList(row));

    service.spreadsheets().values().append(spreadsheetID, range, body)
        .setValueInputOption("USER_ENTERED")
        .setInsertDataOption("INSERT_ROWS")
        .execute();
    System.out.println("Row added successfully");
}
}
