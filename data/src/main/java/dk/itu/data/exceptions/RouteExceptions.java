package dk.itu.data.exceptions;

public class RouteExceptions {
    public class RouteIsNullException extends RuntimeException{
        public RouteIsNullException(String message){
            super(message);
        }
    }
}
