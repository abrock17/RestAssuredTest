import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;
import com.jayway.restassured.specification.RequestSender;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Created by tbrock on 11/7/15.
 */
public class SpellCheckApiTest {

    private final static String APPLICATION_KEY_KEY = "X-Mashape-Key";
    private final static String APPLICATION_KEY = "7B1hOZwwZxmshMxXr74UdmU1ueAFp10Eln5jsnubiBRJBSsl2S";

    @Test
    public void shouldReturnMessageForMissingApplicationKey() {
        ValidatableResponse response = get(when());
        response
                .contentType(ContentType.JSON)
                .body("message", equalTo("Missing Mashape application key. " +
                        "Go to http://docs.mashape.com/api-keys to learn how to get your API application key."));
    }

    @Test
    public void shouldReturnErrorForMissingQueryParameter() {
        ValidatableResponse response = get(given()
                        .header(APPLICATION_KEY_KEY, APPLICATION_KEY)
        );
        response
                .contentType(ContentType.JSON)
                .body("error", equalTo("Need text query parameter"));
    }

    @Test
    public void shouldReturnErrorForMissingQueryParameterWhenAnUnknownParameterIsPassed() {
        ValidatableResponse response = get(given()
                        .header(APPLICATION_KEY_KEY, APPLICATION_KEY)
                        .parameter("unknown", "parameter")
        );
        response
                .contentType(ContentType.JSON)
                .body("error", equalTo("Need text query parameter"));
    }

    @Ignore("can't figure out how to escape special characters")
    @Test
    public void shouldReturnUndefinedSuggestionWhenGobbledygookTextParameterIsPassed() {
        String qBertSwearing = "#\\$%*!!";
        ValidatableResponse response = get(given()
                        .header(APPLICATION_KEY_KEY, APPLICATION_KEY)
                        .parameter("text", qBertSwearing)
        );
        response
                .contentType(ContentType.JSON)
                .body("original", equalTo(qBertSwearing))
                .body("suggestion", equalTo("undefined."))
                .body("corrections.\"#\\$%*!!\"", equalTo(new String[]{"undefined."}));
    }

    @Test
    public void shouldReturnOriginalWithNoCorrectionsForCorrectlySpelledWord() {
        String word = "filament";
        ValidatableResponse response = get(given()
                        .header(APPLICATION_KEY_KEY, APPLICATION_KEY)
                        .parameter("text", word)
        );
        response
                .contentType(ContentType.JSON)
                .body("original", equalTo(word))
                .body("suggestion", equalTo(word))
                .body("corrections", hasToString("{}"));
    }

    @Test
    public void shouldReturnOriginalWithSuggestionAndCorrectionsForIncorrectlySpelledWord() {
        String word = "phillament";
        ValidatableResponse response = get(given()
                        .header(APPLICATION_KEY_KEY, APPLICATION_KEY)
                        .parameter("text", word)
        );
        response
                .contentType(ContentType.JSON)
                .body("original", equalTo(word))
                .body("suggestion", isA(String.class))
                .body("corrections." + word, not(empty()))
                .body("corrections." + word, hasItem("filament"))
                .body("corrections." + word,
                        r -> hasItem(r.jsonPath().getString("suggestion")));
    }

    @Test
    public void shouldReturnOriginalWithSuggestestionAndCorrectionsForMultipleIncorrectlySpelledWords() {
        String sentence = "Mee faill englisch? Thaats umpossible!";
        List<String> words = Arrays.asList(sentence.split("[^\\w]+"));

        ValidatableResponse response = get(given()
                        .header(APPLICATION_KEY_KEY, APPLICATION_KEY)
                        .parameter("text", sentence)
        );
        response
                .contentType(ContentType.JSON)
                .body("original", equalTo(sentence))
                .body("suggestion", isA(String.class));
        String suggestion = response.extract().jsonPath().getString("suggestion");
        assertEquals(words.size(), suggestion.split("[^\\w]+").length);
        words.stream().forEach(word -> {
            response.body("corrections." + word, not(empty()));
            List<String> corrections = response.extract().jsonPath().getList("corrections." + word, String.class);
            assertTrue(corrections.stream()
                    .filter(correction -> suggestion.contains(correction)).count() > 0);
        });
    }

    private ValidatableResponse get(RequestSender requestSender) {
        Response response = requestSender.get("https://montanaflynn-spellcheck.p.mashape.com/check");
        System.out.println("Response : " + response.asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.body(matchesJsonSchemaInClasspath("spellcheck.json"));
        return validatableResponse;
    }
}
