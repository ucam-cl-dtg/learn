package uk.ac.cam.cl.dtg.android.language;

/**
 * 
 * Interface for the answer listener. This interface is implemented by any part
 * of the application that receives answers from the answer components -
 * currently CardEditor and LearningActivity.
 * 
 * 
 * @author Vytautas
 * 
 */
public interface AnswerListener
{
	/**
	 * Called when the correct answer on the answer component is chosen/typed in
	 */
	public void answerCorrect();

	/**
	 * Called when incorrect answer is chosen/typed in.
	 */
	public void answerIncorrect();
}
