package dk.itu;

import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.canvas.events.GLDisposeEvent;
import com.huskerdev.openglfx.canvas.events.GLInitializeEvent;
import com.huskerdev.openglfx.canvas.events.GLRenderEvent;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import dk.itu.gl.*;
import dk.itu.shaders.Poly;
import dk.itu.util.LoggerFactory;
import kotlin.Pair;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static org.lwjgl.opengl.GL46.*;

public class MapComponent extends GLCanvas {
    private final Logger logger = LoggerFactory.getLogger();
    private final State state;
    private final int polygonIndexCount, lineIndexCount;

    public MapComponent(State state, ShapeProcessor.VertexAndIndexData vertexAndIndexData) {
        super(LWJGLExecutor.LWJGL_MODULE);
        this.state = state;
        this.polygonIndexCount = vertexAndIndexData.getPolygonIndices().length;
        this.lineIndexCount = vertexAndIndexData.getLineIndices().length;

        // Events
        addOnInitEvent(e -> this.init(e, vertexAndIndexData));
        addOnRenderEvent(this::render);
        addOnDisposeEvent(this::dispose);
        // Mouse Events
        setupMouseHandlers();
    }

    private void setupMouseHandlers() {
        this.setOnScroll(state::handleScroll);
        this.setOnMousePressed(state::handleMousePressed);
        this.setOnMouseDragged(state::handleMouseDragged);
        this.setOnMouseReleased(state::handleMouseReleased);
    }

    private Shader shader;
    private VertexArrayObject vao;
    private VertexBufferObject vbo;
    private EntityBufferObject ebo;

    private void init(GLInitializeEvent event, ShapeProcessor.VertexAndIndexData vertexAndIndexData) {
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
        vao = new VertexArrayObject(vertexAndIndexData.getVertices());
        vao.bind();

        // Link vbo to vao (bind vbo - define layout - unbind vbo)
        vbo = new VertexBufferObject(vertexAndIndexData.getVertices());
        ebo = new EntityBufferObject(ArrayUtils.addAll(vertexAndIndexData.getPolygonIndices(), vertexAndIndexData.getLineIndices()));

        vao.linkVbo(vbo, layout);

        // Unbind vao - vbo
        vao.unbind();
        vbo.unbind();
        ebo.unbind();
    }

    private void render(GLRenderEvent event) {
//        long start = System.nanoTime();
        // CLEAR
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PREPARE VAO
        shader.activate();
        vao.bind();

        glUniformMatrix4fv(glGetUniformLocation(shader.program, "model"), false, state.getCamera().getModelMatrixFloatArray());
        glUniformMatrix4fv(glGetUniformLocation(shader.program, "view"), false, state.getCamera().getViewMatrixFloatArray());
        glUniformMatrix4fv(glGetUniformLocation(shader.program, "projection"), false, state.getCamera().getProjectionMatrixFloatArray());

//        glDrawRangeElements(GL_TRIANGLES, 0, polygonIndexCount, polygonIndexCount, GL_UNSIGNED_INT, 0);
//        glDrawRangeElements(GL_LINE, polygonIndexCount, polygonIndexCount+lineIndexCount, lineIndexCount, GL_UNSIGNED_INT, 0);
//        glDrawElements(GL_LINES_ADJACENCY, lineIndexCount, GL_UNSIGNED_INT, 0);
//        glDrawElementsBaseVertex(GL_LINE_LOOP, lineIndexCount, GL_UNSIGNED_INT, 0, polygonIndexCount-1);
//        logger.debug("Render loop took {} ms", String.format("%.3f", (System.nanoTime() - start) / 1000000f));
    }

    private void dispose(GLDisposeEvent glDisposeEvent) {
        vao.destroy();
        vbo.destroy();
        ebo.destroy();
        shader.destroy();
    }
}
