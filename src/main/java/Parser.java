import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.List;
import java.util.Map;

public class Parser {
    public Map<String, String> parse(String template, List<String> fillings) {
        Jinjava jinjava = new Jinjava();
        Map<String, String> res = Maps.newHashMap();
        Map<String, String> context = Maps.newHashMap();
        int number = 1;
        int row = 0;
        for (String line : fillings) {
            if (line.isEmpty()) {
                continue;
            }
            if (EmailValidator.getInstance().isValid(line)) {
                res.put(line, jinjava.render(template, context));
                context.clear();
                row = 0;
                number += 1;
            } else {
                row += 1;
                List<String> l = List.of(line.split(": "));
                if (l.size() != 2) {
                    System.out.println("Bad line in batch " + number + " in row " + row);
                    System.exit(1);
                }

                context.put(l.get(0), l.get(1));
            }
        }
        return res;
    }
}
