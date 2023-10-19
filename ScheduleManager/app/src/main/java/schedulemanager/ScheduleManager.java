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
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

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
  private static String spreadsheetID = "";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String totalList = "A2:D";
  public static final String WIP = "Work In Progress - Please Try Again Later...";

  /**
   * Sets the spreadsheet ID basesd on the users entered URL
   */
  public static void sheetIDSetter()
  {
    Scanner user = new Scanner(System.in);
    boolean sheetSet = false;
    while(sheetSet == false)
    {
    
    System.out.println("\nPlease enter your google sheets url");
    String sheetURL = user.nextLine();
    int startIndex = sheetURL.indexOf("/spreadsheets/d/");
    int endIndex = sheetURL.indexOf("/edit");
    if(startIndex > 0 && endIndex > 0)
    {
      spreadsheetID = sheetURL.substring(startIndex+16,endIndex); 
      sheetSet = true;
    }
    else if(startIndex > 0)
      System.out.println("\nERROR. INVALID URL. PLEASE CHANGE SHEET TO EDIT MODE");
    else
      System.out.println("\nERROR. INVALID SHEET URL.");
    }
  }

  /**
   * Authorizes the program with Google's services
   * @return Returns the approved crendentials
   * @throws IOException
   * @throws GeneralSecurityException
   */
  private static Credential authorize() throws IOException, GeneralSecurityException
  {
    InputStream input = SheetsAndJava.class.getResourceAsStream("/credentials.json");
    GoogleClientSecrets clientSecret = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(input));
    List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
    GoogleAuthorizationCodeFlow codeFlow = new GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecret, scopes).setDataStoreFactory(new FileDataStoreFactory(new File("tokens"))).setAccessType("offline").build();
                                                                                
    Credential cred = new AuthorizationCodeInstalledApp(codeFlow,new LocalServerReceiver()).authorize("user");
    return cred;
  }

  /**
   * Establishes Google's Google Sheets service and builds a Google sheets object enabling the program to edit and/or create sheets
   * @return Returns the the Google Sheets object
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static Sheets getSheetService() throws IOException, GeneralSecurityException
  {
    Credential cred = authorize();
    return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, cred).setApplicationName(APPLICATION_NAME).build();
  }

  //Class that holds the current real world date
  public static class CurrentDate
  {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yy");
    String currentDate = LocalDate.now().format(format);
    int currentDay = Integer.parseInt(""+currentDate.charAt(3)+currentDate.charAt(4));
    int currentMonth = Integer.parseInt(""+currentDate.charAt(0)+currentDate.charAt(1));
    int currentYear = Integer.parseInt(""+currentDate.charAt(6)+currentDate.charAt(7));
  }

  //Enables the deletion of rows
  private static class DeleteRow
  {
    int counter = 0;
    BatchUpdateSpreadsheetRequest batchUpdate = new BatchUpdateSpreadsheetRequest();
    Request delete = new Request().setDeleteDimension(new DeleteDimensionRequest().setRange(new DimensionRange().setSheetId(0).setDimension("ROWS").setStartIndex(1).setEndIndex(2)));
    List<Request> deletions = new ArrayList<Request>();
    /**
     * The call that actually deletes the desired row
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void deleteCall() throws IOException, GeneralSecurityException
    {
      deletions.add(delete);
      batchUpdate.setRequests(deletions);
      getSheetService().spreadsheets().batchUpdate(spreadsheetID, batchUpdate).execute();
      counter++;
    }
  }
  /**
   * Calls for the deletion of all expired assignment dates
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static void deleteOldDates() throws IOException, GeneralSecurityException
  {
    //Keeps track of how many rows have been deleted
    int counter = 0;
    //Gets todays date by creating a date object
    CurrentDate date = new CurrentDate();
    //Places each value of the spreadsheet into an array
    ValueRange grab = getSheetService().spreadsheets().values().get(spreadsheetID, totalList).execute();
    List<List<Object>> listDates = grab.getValues();
    //Traverses the spreadsheet array and compares each rows date with the today's date
    if (listDates == null || listDates.isEmpty())
      System.out.println("No data found...");
    else
      for(List row : listDates)
      {
        //Finds the specific rows date
        String setDate = row.get(2).toString();
        //Sets the day, month, and year off of the rows date
        int setMonth = Integer.parseInt(""+setDate.charAt(0)+setDate.charAt(1));
        int setDay = Integer.parseInt(""+setDate.charAt(3)+setDate.charAt(4));
        int setYear = Integer.parseInt(""+setDate.charAt(6)+setDate.charAt(7));
        //Creates a delete row object
        DeleteRow rowRemover = new DeleteRow();
        //If the rows date is older than the current date then the row will be deleted
        if(date.currentYear > setYear)
          rowRemover.deleteCall();
        else if(date.currentMonth > setMonth && date.currentYear == setYear)
          rowRemover.deleteCall();
        else if(date.currentDay > setDay && date.currentYear == setYear && date.currentMonth == setMonth)
          rowRemover.deleteCall();
        rowRemover.counter = counter;
      }
      if(counter == 0)
        System.out.println("\nEvery row is up to date!");
      else
        System.out.println("\n"+counter+" old row(s) have been deleted!");
  }

  public static void menuInputMismatchCatcher(/*Parameter is a static void method*/)//Currently an unused method
  {
    //I need to figure out how to make this possible
    //The key to figuring this out is through lambda expressions
    Scanner user = new Scanner(System.in);
    boolean forward = false;
    while(forward == false)
    {
      try
      {
        //myMethod(user);
      }
      catch(InputMismatchException i)
      {
        System.out.println("\nINVALID INPUT. PLEASE TRY AGAIN.");
        user.nextLine();
      }
    }
  }
  public static void startMenu() throws IOException, GeneralSecurityException
  {
    Scanner user = new Scanner(System.in);
    System.out.println("\nWelcome to the Google Sheets Schedule Mangement System.");
    boolean forward = false;
    while(forward == false)
    {
      try
      {
        System.out.print("\nTo begin, please type your option below.\n(1) - Create a new Google Sheet\n(2) - Use an existing Google Sheet\n(3) - Exit the program\n[Type Here]: ");
        int pick = user.nextInt();
        switch(pick)
        {
          case 1:
            System.out.println(WIP);
            break;
          case 2:
            sheetIDSetter();
            sheetEditMenu();
            forward = true;
            break;
          case 3:
            System.out.println("\nClosing program... Thank you for your time...");
            forward = true;
            user.close();
            break;
          default:
            System.out.println("\nINVALID INPUT. PLEASE TRY AGAIN.");
            break;
        }
      }
      catch(InputMismatchException i)
      {
        System.out.println("\nINVALID INPUT. PLEASE TRY AGAIN.");
        user.nextLine();
      }
    }
  }
  public static void sheetEditMenu() throws IOException, GeneralSecurityException
  {
    Scanner user = new Scanner(System.in);
    boolean forward = false;
    while(forward == false)
    {
      try
      {
        System.out.print("\nWhat would you like to do to this Sheet?\n(1) - Sort by catagory\n(2) - Color by catagory\n(3) - Delete by catagory\n(4) - Go back\n[Type Here]: ");
        int pick = user.nextInt();
        switch(pick)
        {
          case 1:
            System.out.println(WIP);
            break;
          case 2:
            System.out.println(WIP);
            break;
          case 3:
            deleteByCatMenu();
            forward = true;
            break;
          case 4:
            startMenu();
            forward = true;
            break;
          default:
            System.out.println("\nINVALID INPUT. PLEASE TRY AGAIN");
        }
      }
      catch(InputMismatchException i)
      {
        System.out.println("\nINVALID INPUT. PLEASE TRY AGAIN.");
        user.nextLine();
      }
    }
  }
  public static void deleteByCatMenu() throws IOException, GeneralSecurityException
  {
    Scanner user = new Scanner(System.in);
    boolean forward = false;
    while(forward == false)
    {
      try
      {
        System.out.print("\nWhat catagory would you like to delete by?\n(1) - Delete by title\n(2) - Delete by color\n(3) - Delete by expired dates\n(4) - Go back\n[Type Here]: ");
        int pick = user.nextInt();
        switch(pick)
        {
          case 1:
            System.out.println(WIP);
            break;
          case 2:
            System.out.println(WIP);
            break;
          case 3:
            deleteOldDates();
            break;
          case 4:
            sheetEditMenu();
            forward = true;
            break;
          default:
            System.out.println("\nINVALID INPUT. PLEASE TRY AGAIN.");
        }
      }
      catch(InputMismatchException i)
      {
        System.out.println("\nINVALID INPUT. PLEASE TRY AGAIN.");
        user.nextLine();
      }
    }
  }

  public static void main(String[] args) throws IOException, GeneralSecurityException
  {
    startMenu();
  }
}