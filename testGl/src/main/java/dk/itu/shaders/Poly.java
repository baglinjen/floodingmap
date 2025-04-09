package dk.itu.shaders;

public class Poly {
    public static final String vertexShader = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aColor;
            
            uniform mat4 model;
            uniform mat4 view;
            uniform mat4 projection;
            
            out vec3 vertexColor;
        
            void main()
            {
                gl_Position = projection * view * model * vec4(aPos, 1.0);
                vertexColor = aColor;
            }
    """;

    public static final String fragmentShader = """
            #version 330 core
            out vec4 FragColor;
            
            in vec3 vertexColor;
          
            void main()
            {
                FragColor = vec4(vertexColor, 1.0);
            }
    """;
}
