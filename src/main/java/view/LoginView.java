package view;

import controller.UserAuth;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * LoginView — modal dialog that gates CleanView.
 * Has two modes toggled by a link at the bottom:
 *   • Sign In  — authenticates against the users table
 *   • Register — creates a new account, then auto-signs in
 */
public class LoginView extends JDialog {

    // ── Palette ────────────────────────────────────────────────────────────
    private static final Color BG             = new Color(0xF8FAFC);
    private static final Color SURFACE        = Color.WHITE;
    private static final Color BORDER         = new Color(0xE2E8F0);
    private static final Color TEXT_MAIN      = new Color(0x0F172A);
    private static final Color TEXT_MUTED     = new Color(0x64748B);
    private static final Color TEXT_SUBTLE    = new Color(0x94A3B8);
    private static final Color ACCENT         = new Color(0x3B82F6);
    private static final Color ACCENT_BG      = new Color(0xEFF6FF);    //0xEFF6FF
    private static final Color ACCENT_BORDER  = new Color(0xBFDBFE);
    private static final Color ACCENT_HOVER   = new Color(0x2563EB);
    private static final Color ERROR_TEXT     = new Color(0xE11D48);
    private static final Color ERROR_BG       = new Color(0xFFE4E6);
    private static final Color ERROR_BORDER   = new Color(0xFDA4AF);
    private static final Color SUCCESS_TEXT   = new Color(0x16A34A);
    private static final Color SUCCESS_BG     = new Color(0xDCFCE7);
    private static final Color SUCCESS_BORDER = new Color(0x86EFAC);
    private static final Color INPUT_FOCUS    = new Color(0x93C5FD);

    // ── State ──────────────────────────────────────────────────────────────
    private final UserAuth userAuth;
    private boolean loginSuccess = false;
    private boolean registerMode = false;

    // ── Login fields ───────────────────────────────────────────────────────
    private JTextField     loginUsernameField;
    private JPasswordField loginPasswordField;
    private JLabel         loginErrorLabel;
    private JButton        loginBtn;

    // ── Register fields ────────────────────────────────────────────────────
    private JTextField     regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmField;
    private JLabel         regFeedbackLabel;
    private JButton        regBtn;

    // ── Card container (swapped between login / register) ──────────────────
    private JPanel         cardContainer;
    private CardLayout     cardLayout;

    public LoginView(UserAuth userAuth) {
        super((Frame) null, "Distance Monitor — Sign In", true);
        this.userAuth = userAuth;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildRoot());

        getRootPane().registerKeyboardAction(
            e -> { loginSuccess = false; dispose(); },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    /** Shows dialog, blocks, returns true on successful auth. */
    public boolean showAndWait() {
        setVisible(true);
        return loginSuccess;
    }

    // ── Root ───────────────────────────────────────────────────────────────
    private JPanel buildRoot() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 8; i >= 1; i--) {
                    g2.setColor(new Color(15, 23, 42, (int)(180.0 / (i * i))));
                    g2.fill(new RoundRectangle2D.Double(i, i,
                            getWidth()-i, getHeight()-i, 16, 16));
                }
                g2.setColor(SURFACE);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth()-8,getHeight()-8,16,16));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(.5,.5,getWidth()-8.5,getHeight()-8.5,16,16));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(360, 470));
        card.setBorder(new EmptyBorder(0, 0, 8, 8));
        card.add(buildTopBar(), BorderLayout.NORTH);

        // CardLayout swaps login ↔ register panels
        cardLayout    = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);
        cardContainer.add(buildLoginPanel(),    "login");
        cardContainer.add(buildRegisterPanel(), "register");
        card.add(cardContainer, BorderLayout.CENTER);

        wrapper.add(card);
        return wrapper;
    }

    // ── Top bar ────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(20, 24, 0, 24));

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoRow.setOpaque(false);

        JComponent icon = new JComponent() {
            { setPreferredSize(new Dimension(36, 36)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_BG);  g2.fillOval(0,0,35,35);
                g2.setColor(ACCENT);     g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(3,3,29,29);  g2.drawOval(9,9,17,17);
                g2.fillOval(15,15,6,6);
                g2.dispose();
            }
        };

        JLabel appName = new JLabel("Distance Monitor");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        appName.setForeground(TEXT_MAIN);
        logoRow.add(icon); logoRow.add(appName);

        // Drag support
        MouseAdapter drag = new MouseAdapter() {
            Point start;
            @Override public void mousePressed(MouseEvent e)  { start = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                Point loc = LoginView.this.getLocation();
                LoginView.this.setLocation(loc.x+e.getX()-start.x, loc.y+e.getY()-start.y);
            }
        };
        bar.addMouseListener(drag); bar.addMouseMotionListener(drag);
        logoRow.addMouseListener(drag); logoRow.addMouseMotionListener(drag);

        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeBtn.setForeground(TEXT_SUBTLE);
        closeBtn.setContentAreaFilled(false); closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> { loginSuccess = false; dispose(); });
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(ERROR_TEXT); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(TEXT_SUBTLE); }
        });

        bar.add(logoRow, BorderLayout.WEST);
        bar.add(closeBtn, BorderLayout.EAST);
        return bar;
    }

    // ── LOGIN PANEL ────────────────────────────────────────────────────────
    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(22, 28, 16, 28));

        // Heading
        JLabel heading = label("Sign in", new Font("Segoe UI", Font.BOLD, 20), TEXT_MAIN);
        JLabel sub     = label("Enter your credentials to access the dashboard",
                               new Font("Segoe UI", Font.PLAIN, 12), TEXT_MUTED);
        panel.add(heading); panel.add(Box.createVerticalStrut(4)); panel.add(sub);
        panel.add(Box.createVerticalStrut(22));

        // Username
        panel.add(fieldLabel("Username"));
        panel.add(Box.createVerticalStrut(6));
        loginUsernameField = new JTextField();
        styleInput(loginUsernameField);
        panel.add(loginUsernameField);
        panel.add(Box.createVerticalStrut(14));

        // Password
        panel.add(fieldLabel("Password"));
        panel.add(Box.createVerticalStrut(6));
        loginPasswordField = new JPasswordField();
        styleInput(loginPasswordField);
        loginPasswordField.addActionListener(e -> attemptLogin());
        panel.add(loginPasswordField);
        panel.add(Box.createVerticalStrut(16));

        // Error
        loginErrorLabel = feedbackLabel(" ");
        panel.add(loginErrorLabel);
        panel.add(Box.createVerticalStrut(14));

        // Button
        loginBtn = accentButton("Sign In");
        loginBtn.addActionListener(e -> attemptLogin());
        panel.add(loginBtn);
        panel.add(Box.createVerticalStrut(18));

        // Switch to register
        panel.add(switchLink("Don't have an account?", "Create one", () -> switchTo("register")));

        return panel;
    }

    // ── REGISTER PANEL ─────────────────────────────────────────────────────
    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(22, 28, 16, 28));

        JLabel heading = label("Create account", new Font("Segoe UI", Font.BOLD, 20), TEXT_MAIN);
        JLabel sub     = label("Your account will be saved and usable on next login",
                               new Font("Segoe UI", Font.PLAIN, 12), TEXT_MUTED);
        panel.add(heading); panel.add(Box.createVerticalStrut(4)); panel.add(sub);
        panel.add(Box.createVerticalStrut(22));

        // Username
        panel.add(fieldLabel("Username"));
        panel.add(Box.createVerticalStrut(6));
        regUsernameField = new JTextField();
        styleInput(regUsernameField);
        panel.add(regUsernameField);
        panel.add(Box.createVerticalStrut(14));

        // Password
        panel.add(fieldLabel("Password"));
        panel.add(Box.createVerticalStrut(6));
        regPasswordField = new JPasswordField();
        styleInput(regPasswordField);
        panel.add(regPasswordField);
        panel.add(Box.createVerticalStrut(14));

        // Confirm password
        panel.add(fieldLabel("Confirm Password"));
        panel.add(Box.createVerticalStrut(6));
        regConfirmField = new JPasswordField();
        styleInput(regConfirmField);
        regConfirmField.addActionListener(e -> attemptRegister());
        panel.add(regConfirmField);
        panel.add(Box.createVerticalStrut(16));

        // Feedback
        regFeedbackLabel = feedbackLabel(" ");
        panel.add(regFeedbackLabel);
        panel.add(Box.createVerticalStrut(14));

        // Button
        regBtn = accentButton("Create Account");
        regBtn.addActionListener(e -> attemptRegister());
        panel.add(regBtn);
        panel.add(Box.createVerticalStrut(18));

        // Switch back to login
        panel.add(switchLink("Already have an account?", "Sign in", () -> switchTo("login")));

        return panel;
    }

    // ── Switch between panels ──────────────────────────────────────────────
    private void switchTo(String card) {
        cardLayout.show(cardContainer, card);
        registerMode = card.equals("register");
        // Clear fields and feedback when switching
        if (registerMode) {
            regUsernameField.setText(""); regPasswordField.setText("");
            regConfirmField.setText(""); clearFeedback(regFeedbackLabel);
            regUsernameField.requestFocusInWindow();
        } else {
            loginPasswordField.setText(""); clearFeedback(loginErrorLabel);
            loginUsernameField.requestFocusInWindow();
        }
    }

    // ── Login logic ────────────────────────────────────────────────────────
    private void attemptLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showFeedback(loginErrorLabel, "Please enter both username and password.", false);
            return;
        }

        loginBtn.setEnabled(false); loginBtn.setText("Signing in…");
        clearFeedback(loginErrorLabel);

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return userAuth.authenticate(username, password);
            }
            @Override protected void done() {
                try {
                    if (get()) { loginSuccess = true; dispose(); }
                    else {
                        showFeedback(loginErrorLabel, "Invalid username or password.", false);
                        loginPasswordField.setText("");
                        loginPasswordField.requestFocusInWindow();
                        shakeWindow();
                    }
                } catch (Exception ex) {
                    showFeedback(loginErrorLabel, "Error: " + ex.getMessage(), false);
                } finally {
                    loginBtn.setEnabled(true); loginBtn.setText("Sign In");
                }
            }
        }.execute();
    }

    // ── Register logic ─────────────────────────────────────────────────────
    private void attemptRegister() {
        String username = regUsernameField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirm  = new String(regConfirmField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showFeedback(regFeedbackLabel, "Please fill in all fields.", false); return;
        }
        if (username.length() < 3) {
            showFeedback(regFeedbackLabel, "Username must be at least 3 characters.", false); return;
        }
        if (password.length() < 6) {
            showFeedback(regFeedbackLabel, "Password must be at least 6 characters.", false); return;
        }
        if (!password.equals(confirm)) {
            showFeedback(regFeedbackLabel, "Passwords do not match.", false);
            regConfirmField.setText(""); regConfirmField.requestFocusInWindow();
            shakeWindow(); return;
        }

        regBtn.setEnabled(false); regBtn.setText("Creating…");
        clearFeedback(regFeedbackLabel);

        new SwingWorker<UserAuth.RegisterResult, Void>() {
            @Override protected UserAuth.RegisterResult doInBackground() {
                return userAuth.register(username, password);
            }
            @Override protected void done() {
                try {
                    switch (get()) {
                        case SUCCESS:
                            showFeedback(regFeedbackLabel, "Account created! Signing you in…", true);
                            // Short delay then auto-login
                            Timer t = new Timer(900, e -> {
                                loginSuccess = true;
                                dispose();
                            });
                            t.setRepeats(false); t.start();
                            break;
                        case USERNAME_TAKEN:
                            showFeedback(regFeedbackLabel, "Username already taken. Choose another.", false);
                            regUsernameField.requestFocusInWindow();
                            shakeWindow();
                            regBtn.setEnabled(true); regBtn.setText("Create Account");
                            break;
                        case DB_ERROR:
                            showFeedback(regFeedbackLabel, "Database error. Check your connection.", false);
                            regBtn.setEnabled(true); regBtn.setText("Create Account");
                            break;
                    }
                } catch (Exception ex) {
                    showFeedback(regFeedbackLabel, "Error: " + ex.getMessage(), false);
                    regBtn.setEnabled(true); regBtn.setText("Create Account");
                }
            }
        }.execute();
    }

    // ── UI helpers ─────────────────────────────────────────────────────────
    private JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font); l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel fieldLabel(String text) {
        return label(text, new Font("Segoe UI", Font.BOLD, 12), TEXT_MAIN);
    }

    private JLabel feedbackLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setOpaque(true); l.setBackground(SURFACE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return l;
    }

    private void showFeedback(JLabel label, String msg, boolean success) {
        label.setText("  " + (success ? "✔" : "⚠") + "  " + msg);
        label.setForeground(success ? SUCCESS_TEXT : ERROR_TEXT);
        label.setBackground(success ? SUCCESS_BG   : ERROR_BG);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(success ? SUCCESS_BORDER : ERROR_BORDER, 1, true),
            new EmptyBorder(5, 8, 5, 8)));
    }

    private void clearFeedback(JLabel label) {
        label.setText(" "); label.setOpaque(false); label.setBorder(null);
    }

    private void styleInput(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_MAIN); field.setBackground(SURFACE);
        field.setCaretColor(ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(INPUT_FOCUS, 2, true),
                    new EmptyBorder(7, 11, 7, 11)));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1, true),
                    new EmptyBorder(8, 12, 8, 12)));
            }
        });
    }

    private JButton accentButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled()
                    ? (getModel().isRollover() ? ACCENT_HOVER : ACCENT)
                    : new Color(0xCBD5E1));
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),8,8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE); btn.setOpaque(false);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    /** Creates a "Prefix text [link]" row that calls action on click. */
    private JPanel switchLink(String prefix, String linkText, Runnable action) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel pre = new JLabel(prefix);
        pre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pre.setForeground(TEXT_MUTED);

        JLabel link = new JLabel("<html><u>" + linkText + "</u></html>");
        link.setFont(new Font("Segoe UI", Font.BOLD, 12));
        link.setForeground(ACCENT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
            @Override public void mouseEntered(MouseEvent e) { link.setForeground(ACCENT_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { link.setForeground(ACCENT); }
        });

        row.add(pre); row.add(link);
        return row;
    }

    private void shakeWindow() {
        final Point origin = getLocation();
        final int[] offsets = {-8, 8, -6, 6, -4, 4, -2, 2, 0};
        Timer shake = new Timer(25, null);
        final int[] step = {0};
        shake.addActionListener(e -> {
            if (step[0] < offsets.length) { setLocation(origin.x + offsets[step[0]++], origin.y); }
            else { setLocation(origin); shake.stop(); }
        });
        shake.start();
    }
}
