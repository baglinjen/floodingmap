package dk.itu.gl;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

public class VertexLayout {
    private final List<VertexLayoutAttrib> layouts = new ArrayList<>();

    public VertexLayout addVertexAttrib(VertexLayoutAttrib attrib) {
        layouts.add(attrib);
        return this;
    }

    public void enableVertexAttribs() {
        for (VertexLayoutAttrib layout : layouts) {
            layout.enableVertexAttrib();
        }
    }

    public record VertexLayoutAttrib(int index, int size, int type, boolean normalized, int stride, int pointer) {
        public void enableVertexAttrib() {
            glVertexAttribPointer(index, size, type, normalized, stride, pointer);
            glEnableVertexAttribArray(index);
        }
    }
}
