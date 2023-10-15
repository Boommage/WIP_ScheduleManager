package schedulemanager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.checkerframework.checker.units.qual.g;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.ValueRange;

public class ScheduleManager
{
  private static Sheets sheetService;
  private static final String APPLICATION_NAME = "TCU SCHEDULE MANAGER";
  public static final String SPREADSHEET_ID = "1Jpj5GbpAXukb07nTTKnDOMspYc-BESg8knN8FHBe6As";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String totalList = "A2:D";

  private static Credential authorize() throws IOException, GeneralSecurityException
  {
    InputStream input = SheetsAndJava.class.getResourceAsStream("/credentials.json");
    GoogleClientSecrets clientSecret = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(input));
    List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
    GoogleAuthorizationCodeFlow codeFlow = new GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecret, scopes).setDataStoreFactory(new FileDataStoreFactory(new File("tokens"))).setAccessType("offline").build();
                                                                                
    Credential cred = new AuthorizationCodeInstalledApp(codeFlow,new LocalServerReceiver()).authorize("user");
    return cred;
  }
  
  public static Sheets getSheetService() throws IOException, GeneralSecurityException
  {
    Credential cred = authorize();
    return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, cred).setApplicationName(APPLICATION_NAME).build();
  }
  /**
   * Finds the real world current date
   * @return returns the current date
   */
  public static String currentDateFinder()
  {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yy");
    String currentDate = LocalDate.now().format(format);
    return currentDate;
  }
  /**
   * Finds the real world current day
   * @return returns the current day
   */
  public static int currentDayFinder()
  {
    String date = currentDateFinder();
    int currentDay = Integer.parseInt(""+date.charAt(3)+date.charAt(4));
    return currentDay;
  }
  /**
   * Finds the real world current month
   * @return returns the current month
   */
  public static int currentMonthFinder()
  {
    String date = currentDateFinder();
    int currentMonth = Integer.parseInt(""+date.charAt(0)+date.charAt(1));
    return currentMonth;
  }
  /**
   * Finds the real world current year
   * @return returns the current year
   */
  public static int currentYearFinder()
  {
    String date = currentDateFinder();
    int currentYear = Integer.parseInt(""+date.charAt(6)+date.charAt(7));
    return currentYear;
  }
  /**
   * Deletes all expired assignment dates
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static void deleteOldDates() throws IOException, GeneralSecurityException
  {
    //Declares the current day, month, and year
    int currentMonth = currentMonthFinder();
    int currentDay = currentDayFinder();
    int currentYear = currentYearFinder();

    //Places each value of the spreadsheet into an array
    ValueRange grab = getSheetService().spreadsheets().values().get(SPREADSHEET_ID, totalList).execute();
    List<List<Object>> dates = grab.getValues();
    
    //Traverses the array and compares each rows date with the current date
    if (dates == null || dates.isEmpty())
      System.out.println("No data found...");
    else
      for(List row : dates)
      {
        //Finds the specific rows date
        String setDate = row.get(2).toString();
        //Sets the day, month, and year off of the rows date
        int setMonth = Integer.parseInt(""+setDate.charAt(0)+setDate.charAt(1));
        int setDay = Integer.parseInt(""+setDate.charAt(3)+setDate.charAt(4));
        int setYear = Integer.parseInt(""+setDate.charAt(6)+setDate.charAt(7));

        //Sets necessary values for the deletion of a row
        BatchUpdateSpreadsheetRequest batchUpdate = new BatchUpdateSpreadsheetRequest();
        Request delete = new Request().setDeleteDimension(new DeleteDimensionRequest().setRange(new DimensionRange().setSheetId(0).setDimension("ROWS").setStartIndex(1).setEndIndex(2)));
        List<Request> deletions = new ArrayList<Request>();

        //If the rows date is older than the current date then the row will be deleted
        if(currentYear > setYear)
          deleteCall(delete, deletions, batchUpdate);
        else if(currentMonth > setMonth && currentYear == setYear)
          deleteCall(delete, deletions, batchUpdate);
        else if(currentDay > setDay && currentYear == setYear && currentMonth == setMonth)
          deleteCall(delete, deletions, batchUpdate);
      }
  }
  private static void deleteCall(Request r, List<Request> l, BatchUpdateSpreadsheetRequest b) throws IOException, GeneralSecurityException
  {
    l.add(r);
    b.setRequests(l);
    getSheetService().spreadsheets().batchUpdate(SPREADSHEET_ID, b).execute();
  }

  public static void main(String[] args) throws IOException, GeneralSecurityException
  {
    deleteOldDates();
  }
}