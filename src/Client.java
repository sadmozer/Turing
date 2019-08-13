import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    private static int portRegistro = 6000;
    public static void main (String[] args) {
        System.out.println("[CLIENT]: Avvio..");
        IRegistratore registratoreRemoto;
        Operazione esitoRegistra;
        try {
            Registry reg = LocateRegistry.getRegistry(portRegistro);
            registratoreRemoto = (IRegistratore) reg.lookup("Registratore");
            esitoRegistra = registratoreRemoto.registra("Pippo", "topolinatiamo");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            System.exit(1);
        }


    }
}
