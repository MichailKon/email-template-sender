import org.json.JSONObject;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "e-mail sender", mixinStandardHelpOptions = true,
        description = "Takes template file and file with fillings+emails, render and send letters")
public class Main implements Callable<Integer> {
    public static List<String> readLines(Path path) throws IOException {
        return Files.newBufferedReader(path).lines().toList();
    }

    @CommandLine.Parameters(index = "0", description = """
            json with structure:
            {
              name: ,
              password: ,
            }""")
    private Path credentialsFile;

    @CommandLine.Parameters(index = "1", description = "file with template")
    private Path templateFile;

    @CommandLine.Parameters(index = "2", description = """
            file with fillings with structure:
            place1: fill1
            ...
            email1
            place2: fill2
            ...
            email2
            ...""")
    private Path fillingsFile;

    @Override
    public Integer call() throws Exception {
        String template = Files.readString(templateFile);
        List<String> fillings = readLines(fillingsFile);
        Map<String, String> res = new Parser().parse(template, fillings);

        JSONObject credentials = new JSONObject(String.join("\n", Files.readAllLines(credentialsFile)));
        System.out.println(credentials);
        new Sender().sendMails(res,
                String.valueOf(credentials.get("name")), String.valueOf(credentials.get("password")));
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
