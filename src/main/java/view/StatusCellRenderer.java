package view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * StatusColorRenderer.
 * Uses consistent uppercase status strings matching DistanceModel.
 */
public class StatusCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {
            String status = table.getModel().getValueAt(row, 2).toString().toUpperCase();
            switch (status) {
                case "SAFE":
                    c.setBackground(new Color(144, 238, 144));
                    c.setForeground(new Color(20, 100, 20));
                    break;
                case "WARNING":
                    c.setBackground(new Color(255, 200, 120));
                    c.setForeground(new Color(120, 70, 0));
                    break;
                case "CRITICAL":
                    c.setBackground(new Color(255, 120, 120));
                    c.setForeground(new Color(140, 0, 0));
                    break;
                default:
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
            }
        }
        return c;
    }
}
