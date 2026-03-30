import com.formdev.flatlaf.FlatLightLaf;
import controller.AppController;
import controller.BeepController;
import controller.DatabaseController;
import controller.UserAuth;
import model.DistanceModel;
import view.CleanView;
//import view.GraphView;
import view.LoginView;
//import view.MainView;
//import view.RadarView;

/**
 * Entry point — wires Model, Views, and Controllers together.
 *
 * MVC wiring order:
 *  1. Create the Model
 *  2. Connect to DB
 *  3. Show LoginView — gate CleanView behind authentication
 *  4. Create all Views (register as listeners on the Model)
 *  5. Create Controllers (given the Model to drive)
 *  6. Show Views
 *  7. Start Controllers
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Model ───────────────────────────────────────────────────────
        DistanceModel model = new DistanceModel();

        // ── 2. Database (must connect before login check) ──────────────────
        DatabaseController dbController = new DatabaseController();
        dbController.connect();

        // ── 3. Login gate for CleanView ────────────────────────────────────
        //   LoginView is shown on the EDT; we block here until it closes.
        UserAuth userAuth = new UserAuth(dbController);
        final boolean[] loginOk = {false};

        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try { FlatLightLaf.setup(); } catch (Exception ignored) {}
                LoginView login = new LoginView(userAuth);
                loginOk[0] = login.showAndWait();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!loginOk[0]) {
            // User closed the dialog without signing in — shut down cleanly
            System.out.println("Login cancelled. Exiting.");
            dbController.disconnect();
            System.exit(0);
        }

        // ── 4. Views (register on model) ───────────────────────────────────
//        GraphView graphView = new GraphView();
//        RadarView radarView = new RadarView();
//        MainView  mainView  = new MainView(model, graphView, radarView, userAuth);
        CleanView cleanView = new CleanView(model, userAuth);   // ← protected by login
        model.addListener(cleanView);
//        model.addListener(graphView);
//        model.addListener(radarView);
//        model.addListener(mainView);

        // ── 5. Controllers ─────────────────────────────────────────────────
        BeepController beepController = new BeepController();
        model.addListener(dbController);
        model.addListener(beepController);

        AppController appController = new AppController(model, dbController);

        // ── 6. Show views ──────────────────────────────────────────────────
        javax.swing.SwingUtilities.invokeLater(() -> {
//            mainView.show();
            cleanView.show();
        });

        // ── 7. Start sensor loop ───────────────────────────────────────────
        appController.start();
        beepController.start();

        // Graceful shutdown on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            appController.stop();
            beepController.stop();
        }));
    }
}
