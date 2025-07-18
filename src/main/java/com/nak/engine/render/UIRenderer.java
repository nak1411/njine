package com.nak.engine.render;

import static org.lwjgl.opengl.GL11.*;

class UIRenderer {
    public void renderCrosshair() {
        glColor3f(1.0f, 1.0f, 1.0f);
        glLineWidth(2.0f);

        glBegin(GL_LINES);
        // Horizontal line
        glVertex2f(960 - 15, 540);
        glVertex2f(960 + 15, 540);
        // Vertical line
        glVertex2f(960, 540 - 15);
        glVertex2f(960, 540 + 15);
        glEnd();

        glLineWidth(1.0f);
    }

    public void renderDebugInfo(String debugText) {
        // In a real implementation, this would render text
        // For now, we'll just indicate where the text would be
        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        glBegin(GL_QUADS);
        glVertex2f(10, 10);
        glVertex2f(400, 10);
        glVertex2f(400, 300);
        glVertex2f(10, 300);
        glEnd();

        // Text would be rendered here using a font system
        // For console output instead:
        if (System.currentTimeMillis() % 2000 < 100) { // Print every 2 seconds, briefly
            System.out.println(debugText);
        }
    }

    public void cleanup() {
        // Cleanup UI resources
    }
}
