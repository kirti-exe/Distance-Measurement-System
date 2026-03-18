import com.formdev.flatlaf.FlatLightLaf;
import controller.AppController;
import controller.DatabaseController;
import model.DistanceModel;
import view.CleanView;
import view.GraphView;
import view.MainView;
import view.RadarView;

/**
 * Entry point — wires Model, Views, and Controllers together.
 *
 * MVC wiring order:
 *  1. Create the Model
 *  2. Create all Views (register as listeners on the Model)
 *  3. Create Controllers (given the Model to drive)
 *  4. Show Views
 *  5. Start Controllers
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Model ───────────────────────────────────────────────────────
        DistanceModel model = new DistanceModel();

        // ── 2. Views (register on model) ───────────────────────────────────
        GraphView graphView = new GraphView();
        RadarView radarView = new RadarView();
        MainView  mainView  = new MainView(model, graphView, radarView);
        CleanView cleanView = new CleanView(model);

        model.addListener(graphView);   // chart updates
        model.addListener(radarView);   // radar updates
        model.addListener(mainView);    // original dashboard updates
        model.addListener(cleanView);   // clean dashboard updates

        // ── 3. Controllers ─────────────────────────────────────────────────
        DatabaseController dbController = new DatabaseController();
        dbController.connect();
        model.addListener(dbController); // auto-saves every reading

        AppController appController = new AppController(model, dbController);

        // ── 4. Show views ──────────────────────────────────────────────────
        javax.swing.SwingUtilities.invokeLater(() -> {
            try { FlatLightLaf.setup(); } catch (Exception ignored) {}
            mainView.show();
            cleanView.show();
        });

        // ── 5. Start sensor loop ───────────────────────────────────────────
        appController.start();

        // Graceful shutdown on exit
        Runtime.getRuntime().addShutdownHook(new Thread(appController::stop));
    }
}
