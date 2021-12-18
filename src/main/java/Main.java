import org.neo4j.driver.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    static Session session;

    static List<String> getMailsFrom(String user) {

        String q = "match (:User { name: $user })-[:SENT]-(e:Email) " +
                "return e";

        Map<String, Object> params = Map.of("user", user);

        var result = session.run(q, params);
        return result.list().stream().map(r -> r.get("content").asString()).collect(Collectors.toList());
    }

    static void sendMail(String from, String to, String content) {

        String q = "merge (f:User { name: $fromName }) " +
                "merge (t:User { name: $toName }) " +
                "create (f)-[:SENT]->(e:Email { content: $content })-[:TO]->(t)";

        Map<String, Object> params = Map.of(
                "fromName", from,
                "toName", to,
                "content", content
        );

        session.run(q, params);
    }

    public static void main(String[] args) throws IOException {

        var driver= GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "Password"));
        session = driver.session();

        var in = new BufferedReader(new InputStreamReader(System.in));

        boolean exit = false;
        while(!exit) {
            var input = in.readLine();

            String command = input.split("\s+", 2)[0].toLowerCase();

            switch (command) {
                case "exit" -> exit = true;
                case "send" -> {
                    var sendArgs = input.split("\s+", 4);
                    sendMail(sendArgs[1], sendArgs[2], sendArgs[3]);
                }
            }

        }

        driver.close();
    }

}
