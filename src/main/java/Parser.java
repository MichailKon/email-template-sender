import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.RenderResult;
import org.apache.commons.validator.routines.EmailValidator;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Parser {
    public String validateEmails(JSONObject fillings) {
        Iterator<String> keysIt = fillings.keys();
        while (keysIt.hasNext()) {
            String email = keysIt.next();
            if (!EmailValidator.getInstance().isValid(email)) {
                return email;
            }
        }
        return "";
    }

    public String checkSameKeys(JSONObject fillings) {
        Iterator<String> keysIt = fillings.keys();
        String firstEmail = null;
        Set<String> firstData = null;
        while (keysIt.hasNext()) {
            String email = keysIt.next();
            if (firstEmail == null) {
                firstEmail = email;
                firstData = fillings.getJSONObject(email).keySet();
            }
            if (!firstData.equals(fillings.getJSONObject(email).keySet())) {
                return firstEmail + " " + email;
            }
        }
        return "";
    }

    public String validateFillings(String template, JSONObject fillings) {
        JinjavaConfig jc = JinjavaConfig.newBuilder().withFailOnUnknownTokens(true).build();
        Jinjava jinjava = new Jinjava(jc);
        Iterator<String> keysIt = fillings.keys();
        while (keysIt.hasNext()) {
            String email = keysIt.next();
            Map<String, Object> context = fillings.getJSONObject(email).toMap();
            RenderResult renderResult = jinjava.renderForResult(template, context);
            if (renderResult.hasErrors()) {
                return email;
            }
        }
        return "";
    }

    public Map<String, String> parse(String template, JSONObject fillings) {
        Jinjava jinjava = new Jinjava();
        Map<String, String> res = new java.util.HashMap<>(Map.of());
        Iterator<String> keysIt = fillings.keys();
        while (keysIt.hasNext()) {
            String email = keysIt.next();
            res.put(email, jinjava.render(template, fillings.getJSONObject(email).toMap()));
        }
        return res;
    }
}
