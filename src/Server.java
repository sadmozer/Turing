import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;


public class Server {
    public static void main(String[] args) {
        try {
            Registratore registratore = new Registratore();
            LocateRegistry.createRegistry(6000);
            Registry reg = LocateRegistry.getRegistry(6000);
            reg.rebind("Registratore", registratore);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("[SERVER]: Servizio registratore pronto.");

    }
}
