package dk.itu.gl;

import static org.lwjgl.opengl.GL46.*;

// Constructor that generates a Vertex Buffer Object and links it to vertices
public class VertexBufferObject {
    public final int vbo;

    public VertexBufferObject(float[] vertices) {
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
    }

    public void bind() {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
    }

    public void unbind() {
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void destroy() {
        glDeleteBuffers(vbo);
    }
}
