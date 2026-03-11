import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import  java.awt.*;

public class StatusColorRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        Component c= super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        String status = table.getModel().getValueAt(row, 2).toString();

        if(status.equalsIgnoreCase("safe")){
            c.setBackground(new Color(144,238,144));    // light greeen
        }
        else if(status.equalsIgnoreCase("warning")){
            c.setBackground(new Color(255,200,120));    // orange
        }
        else if(status.equalsIgnoreCase("critical")){
            c.setBackground(new Color(255,120,120));    // red
        }

        return c;
    }
}
