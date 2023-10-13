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
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

public class ScheduleManager
{
  private static Sheets sheetService;
  private static final String APPLICATION_NAME = "TCU SCHEDULE MANAGER";
  private static final String SPREADSHEET_ID = "1Jpj5GbpAXukb07nTTKnDOMspYc-BESg8knN8FHBe6As";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

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

  public static ArrayList<List<Object>> dateEdit() throws IOException, GeneralSecurityException
  {
    ArrayList<List<Object>> newDates = new ArrayList<List<Object>>();
    DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yy");
    String currentDate = LocalDate.now().format(format);
    String setDate;
    int currentMonth = Integer.parseInt(""+currentDate.charAt(0)+currentDate.charAt(1));
    int setDay;
    int currentDay = Integer.parseInt(""+currentDate.charAt(3)+currentDate.charAt(4));
    int setMonth;
    int currentYear = Integer.parseInt(""+currentDate.charAt(6)+currentDate.charAt(7));
    int setYear;
    sheetService = getSheetService();
    String totalList = "A2:D";
    ValueRange grab = sheetService.spreadsheets().values().get(SPREADSHEET_ID, totalList).execute();
    List<List<Object>> dates = grab.getValues();

    if (dates == null || dates.isEmpty())
      System.out.println("No data found...");
    else
      for(List row : dates)
      {
        setDate = row.get(2).toString();
        setMonth = Integer.parseInt(""+setDate.charAt(0)+setDate.charAt(1));
        setDay = Integer.parseInt(""+setDate.charAt(3)+setDate.charAt(4));
        setYear = Integer.parseInt(""+setDate.charAt(6)+setDate.charAt(7));
        if(currentYear < setYear)
          newDates.add(row);
        else if(currentMonth < setMonth && currentYear == setYear)
          newDates.add(row);
        else if(currentDay < setDay && currentYear == setYear && currentMonth == setMonth)
          newDates.add(row);
      }
      if (dates == null || dates.isEmpty())
        System.out.println("No data found...");
      else
        for(List row : newDates)
        {
          System.out.println(row);
        }
      return newDates;
  }
  public static void rowReplacer() throws IOException, GeneralSecurityException 
  {
    //ValueRange newRowText = new ValueRange().setValues(Arrays.asList(Arrays.asList("Changed")));
    ValueRange newRowText = new ValueRange().setValues(dateEdit());
    UpdateValuesResponse rowchanger = sheetService.spreadsheets().values().update(SPREADSHEET_ID,"A68", newRowText).setValueInputOption("RAW").execute();
  }

  public static void main(String[] args) throws IOException, GeneralSecurityException
  {
    System.out.println("GAMEING YAHOOO!!");
    //dateEdit();
    rowReplacer();
  }
}