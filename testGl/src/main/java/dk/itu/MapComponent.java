package dk.itu;

import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.canvas.events.GLDisposeEvent;
import com.huskerdev.openglfx.canvas.events.GLInitializeEvent;
import com.huskerdev.openglfx.canvas.events.GLRenderEvent;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import dk.itu.gl.*;
import dk.itu.shaders.Poly;
import glm_.mat4x4.Mat4;

import static glm_.Java.glm;
import static org.lwjgl.opengl.GL46.*;

public class MapComponent extends GLCanvas {
    private State state;
    private int indexCount;

    public MapComponent(State state, float[] vertices, int[] indices) {
        super(LWJGLExecutor.LWJGL_MODULE);
        this.state = state;
        this.indexCount = indices.length;
        addOnInitEvent(e -> this.init(e, vertices, indices));
        addOnRenderEvent(this::render);
        addOnDisposeEvent(this::dispose);
    }

    private final float sqrt3 = (float) Math.sqrt(3);

    private Shader shader;
    private VertexArrayObject vao;
    private VertexBufferObject vbo;
    private EntityBufferObject ebo;

    private void init(GLInitializeEvent event, float[] vertices, int[] indices) {
        glViewport(0, 0, 800, 800);

        shader = new Shader(Poly.vertexShader, Poly.fragmentShader);
        VertexLayout layout = new VertexLayout();
        layout
                .addVertexAttrib(
                        new VertexLayout.VertexLayoutAttrib(0, 3, GL_FLOAT, false, 6*Float.BYTES, 0)
                )
                .addVertexAttrib(
                        new VertexLayout.VertexLayoutAttrib(1, 3, GL_FLOAT, false, 6*Float.BYTES, 3*Float.BYTES)
                );

        // Create vao and bind to modify it
        vao = new VertexArrayObject(vertices);
        vao.bind();

        // Link vbo to vao (bind vbo - define layout - unbind vbo)
        vbo = new VertexBufferObject(vertices);
        ebo = new EntityBufferObject(indices);

        vao.linkVbo(vbo, layout);

        // Unbind vao - vbo
        vao.unbind();
        vbo.unbind();
        ebo.unbind();
    }

    private void render(GLRenderEvent event) {
        // CLEAR
        glClearColor(1, 1, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PREPARE VAO
        shader.activate();
        vao.bind();

        // Model
        var model = new Mat4(1.0f);
        // View
        var view = new Mat4(1.0f)
                .translate(-10.40289f, -55.959747f, 0.0f)
                .translate(0.0f, 0.0f, -0.1f);
//                .translate(-10.4f, -55.9f, -10.0f); // Initial
//                .translate(state.getTranslateX(), state.getTranslateY(), state.getZoom());
        // Projection
        var perspective = glm.perspective(glm.radians(45.0f), 800.0f/600.0f, 0.0f, 100.0f);

        glUniformMatrix4fv(glGetUniformLocation(shader.program, "model"), false, model.toFloatArray());
        glUniformMatrix4fv(glGetUniformLocation(shader.program, "view"), false, view.toFloatArray());
        glUniformMatrix4fv(glGetUniformLocation(shader.program, "projection"), false, perspective.toFloatArray());

        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, ebo.id);
    }

    private void dispose(GLDisposeEvent glDisposeEvent) {
        vao.destroy();
        vbo.destroy();
        ebo.destroy();
        shader.destroy();
    }
}
