import org.neo4j.driver.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    static Session session;

    static void send(String from, String to, String content) {

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

    static void cc(String id, String user) {

        String q = "match (email:Email { id: $id }), (user:User { name: $user }) " +
                "create (email)-[:CC]->(user)";

        Map<String, Object> params = Map.of(
                "id", id,
                "user", user
        );

        session.run(q, params);
    }

    static List<String> outbox(String user) {

        String q = "match (:User { name: $user })-[:SENT]-(e:Email) " +
                "return e";

        Map<String, Object> params = Map.of("user", user);

        var result = session.run(q, params);
        return result.list().stream().map(r -> r.get("content").asString()).collect(Collectors.toList());
    }

    static void rename(String user, String newName) {

         String q = "match (u:User { name: $user }) " +
                 "set u.name = $newName";

         Map<String, Object> params = Map.of(
                 "user", user,
                 "newName", newName);

         session.run(q, params);
    }

    static void markSpam() {

        String q = "match (:User)-[:SENT]->(email:Email)-[:TO]->(:User) " +
                "where (email)-[:CC]->(:User)-[:ALIAS_OF]->(:User) " +
                "set email:SUSP, email.marked = Date() ";

        session.run(q);
    }

    public static void main(String[] args) throws IOException {

        var driver= GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "Password"));
        session = driver.session();

        var in = new BufferedReader(new InputStreamReader(System.in));

        boolean exit = false;
        while(!exit) {
            var input = in.readLine();

            String[] commandAndArgs = input.split("\s+", 2);
            String command = commandAndArgs[0].toLowerCase();
            String arguments = commandAndArgs[1];

            switch (command) {
                case "exit" -> exit = true;
                case "send" -> {
                    var sendArgs = arguments.split("\s+", 3);
                    send(sendArgs[0], sendArgs[1], sendArgs[2]);
                }
                case "cc" -> {
                    var ccArgs = arguments.split("\s+", 2);
                    cc(ccArgs[0], ccArgs[1]);
                }
                case "outbox" -> {
                    for(String mail: outbox(arguments)) {
                        System.out.println(mail);
                    }
                }
                case "rename" -> {
                    var renameArgs = arguments.split("\s+", 2);
                    rename(renameArgs[0], renameArgs[1]);
                }
                case "markSpam" -> markSpam();
            }

        }

        driver.close();
    }

}
