package dk.itu.gl;

import static org.lwjgl.opengl.GL46.*;

public class VertexArrayObject {
    public final int id;

    public VertexArrayObject(float[] vertices) {
        this.id = glGenVertexArrays();
    }

    public void linkVbo(VertexBufferObject vbo, VertexLayout layout) {
        vbo.bind();

        layout.enableVertexAttribs();

        vbo.unbind();
    }

    public void bind() {
        glBindVertexArray(id);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    public void destroy() {
        glDeleteVertexArrays(id);
    }
}
