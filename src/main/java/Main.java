import org.json.JSONObject;
import picocli.CommandLine;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "e-mail sender", mixinStandardHelpOptions = true,
        description = "Takes template file and file with fillings+emails, render and send letters")
public class Main implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = """
            json file with all the info with structure:
            {
              credentials: path_to_credentials.json,
              template: path_to_template,
              title: mail title (if not presented then ""),
              fillings: {
                email1: {
                  fill1: ...,
                  fill2: ...
                },
                email2: {
                  fill1: ...,
                  fill2: ...
                }
              }
            }""")
    private Path oneJsonFile;

    @CommandLine.Option(names = "--check-same-keys", description = "Checks all the fillings for the same structure")
    boolean checkSameKeys;

    @CommandLine.Option(names = "--no-check-fillings",
            description = "Skip check that every placeholder has full matching for every email")
    boolean skipCheckFillings;

    @CommandLine.Option(names = "--no-check-emails",
            description = "Skip check that every placeholder has full matching for every email")
    boolean skipCheckEmails;

    Map<String, String> getCredData(JSONObject allInfo) throws java.io.IOException {
        String name, password;
        Path credPath;
        if (!allInfo.has("credentials")) {
            System.out.println("Can't find field credentials");
        }
        credPath = Path.of(allInfo.getString("credentials"));

        JSONObject credentials = new JSONObject(String.join("\n", Files.readAllLines(credPath)));

        if (!credentials.has("name") || !credentials.has("password")) {
            System.out.println("Can't find name and/or password in credentials file");
        }
        name = credentials.getString("name");
        password = credentials.getString("password");

        return Map.of("name", name, "password", password);
    }

    String getTemplate(JSONObject allInfo) throws IOException {
        Path tPath;
        if (!allInfo.has("template")) {
            System.out.println("Can't find template path in json file");
        }
        tPath = Path.of(allInfo.getString("template"));

        return Files.readString(tPath);
    }

    JSONObject getFillings(JSONObject allInfo) {
        if (!allInfo.has("fillings")) {
            System.out.println("Can't find any fillings");
            System.exit(1);
        }
        return allInfo.getJSONObject("fillings");
    }

    String getTitle(JSONObject allInfo, Scanner scanner) {
        if (allInfo.has("title")) {
            return allInfo.getString("title");
        }
        System.out.println("No title. Continue (y/n)?: ");
        while (true) {
            String res = scanner.nextLine().toLowerCase(Locale.ROOT);
            if (res.equals("y")) {
                System.out.println("OK");
                break;
            }
            if (res.equals("n")) {
                System.out.println("OK");
                System.exit(1);
            }
            System.out.println("Expected y/n, got " + res + ". Repeat (y/n): ");
        }
        return "";
    }

    @Override
    public Integer call() throws MessagingException, IOException {
        Scanner scanner = new Scanner(System.in);

        JSONObject allInfo;
        try {
            allInfo = new JSONObject(String.join("\n", Files.readAllLines(oneJsonFile)));
        } catch (java.io.IOException e) {
            System.out.println("Can't find json file");
            return 1;
        }
        Map<String, String> credentials = getCredData(allInfo);
        String template = getTemplate(allInfo);
        JSONObject fillings = getFillings(allInfo);
        String title = getTitle(allInfo, scanner);

        Parser parser = new Parser();

        String valResult;
        if (skipCheckEmails) {
            System.out.println("Skipping checking emails");
        } else {
            valResult = parser.validateEmails(fillings);
            if (!valResult.isEmpty()) {
                System.out.println("This key is not an email: " + valResult);
                return 1;
            }
            System.out.println("Emails correct");
        }

        if (skipCheckFillings) {
            System.out.println("Skipping checking fillings");
        } else {
            valResult = parser.validateFillings(template, fillings);
            if (!valResult.isEmpty()) {
                System.out.println("Fillings for " + valResult + " not full");
                return 1;
            }
            System.out.println("Fillings correct");
        }

        if (checkSameKeys) {
            valResult = parser.checkSameKeys(fillings);
            if (!valResult.isEmpty()) {
                System.out.println("These email have different keys: " + valResult);
                return 1;
            }
            System.out.println("Keys sets are equal");
        }
        Sender sender = new Sender();
        sender.sendMails(parser.parse(template, fillings), credentials, title);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
