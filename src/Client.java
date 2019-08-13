import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    public static void main (String[] args) {
        System.out.println("Sono il client!");
        IRegistratore registratoreRemoto;
        Remote remoteObj;
        Map<String, Utente> utentiRegistrati;
        try {
            Registry reg = LocateRegistry.getRegistry(6000);
            registratoreRemoto = (IRegistratore) reg.lookup("REGISTRATION-SERVER");
            registratoreRemoto.register("Pippo", "topolinatiamo");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
