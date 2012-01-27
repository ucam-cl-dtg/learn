package uk.ac.cam.cl.dtg.android.language;


/**
 * Exception thrown when a resource is not found
 * 
 * @author Daniel Thomas (drt24)
 * 
 */
public class ResourceNotFoundException extends Exception {
  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(Exception e) {
    super(e);
  }

  private static final long serialVersionUID = 1L;

}
