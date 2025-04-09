package dk.itu.gl;

import static org.lwjgl.opengl.GL46.*;

public class Shader {
    public final int program;
    public Shader(String vertexShaderCode, String fragmentShaderCode) {
        // Create vertex shader - set source - compile
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderCode);
        glCompileShader(vertexShader);

        // Create fragment shader - set source - compile
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderCode);
        glCompileShader(fragmentShader);

        this.program = glCreateProgram();

        // Add shaders to program
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        // Link shaders in program
        glLinkProgram(program);

        // Delete shaders since they are in the shader program now
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void activate() {
        glUseProgram(program);
    }

    public void destroy() {
        glDeleteProgram(program);
    }
}
