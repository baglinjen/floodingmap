package dk.itu.gl;

import static org.lwjgl.opengl.GL46.*;

public class EntityBufferObject {
    public final int id;

    public EntityBufferObject(int[] indices) {
        id = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
    }

    public void bind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
    }

    public void unbind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void destroy() {
        glDeleteBuffers(id);
    }
}