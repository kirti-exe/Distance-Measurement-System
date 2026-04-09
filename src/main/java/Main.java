import com.formdev.flatlaf.FlatLightLaf;
import controller.*;
import model.DistanceModel;
import view.CleanView;
import view.LoginView;
import view.SplashScreen;

public class Main {

    public static void main(String[] args) {

        // Apply saved theme before ay UI is created
        boolean savedDark = view.CleanView.loadThemePreference();
        try {
            if (savedDark) com.formdev.flatlaf.FlatDarkLaf.setup();
            else           com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ignored) {}

        // ── 1. Model ───────────────────────────────────────────────────────
        DistanceModel model = new DistanceModel();

        // ── 2. Database (must connect before login check) ──────────────────
        DatabaseController dbController = new DatabaseController();
        dbController.connect();

        // 2.5. Splash Screen
        javax.swing.SwingUtilities.invokeLater(() -> {
//            try { FlatLightLaf.setup(); } catch (Exception ignored) {}
            SplashScreen splash = new SplashScreen() {
                @Override
                protected void onSplashFinished() {
                    // Run startApp on a background thread — NOT the EDT
                    new Thread(() -> startApp(model, dbController), "StartupThread").start();
                }
            };
            splash.showAndWait();
        });
    }

    private static void startApp(DistanceModel model, DatabaseController dbController) {
        UserAuth userAuth = new UserAuth(dbController);
        final boolean[] loginOk = {false};

        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
//                try { FlatLightLaf.setup(); } catch (Exception ignored) {}
                LoginView login = new LoginView(userAuth);
                loginOk[0] = login.showAndWait();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!loginOk[0]) {
            System.out.println("Login cancelled. Exiting.");
            dbController.disconnect();
            System.exit(0);
        }

        SosController sosController = new SosController(model);
        CleanView cleanView = new CleanView(model, userAuth, sosController);
        model.addListener(cleanView);

        BeepController beepController = new BeepController();
        model.addListener(dbController);
        model.addListener(beepController);

        AppController appController = new AppController(model, dbController);

        javax.swing.SwingUtilities.invokeLater(cleanView::show);

        appController.start();
        beepController.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            appController.stop();
            beepController.stop();
        }));
    }
}
