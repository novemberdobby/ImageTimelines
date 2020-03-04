package novemberdobby.teamcity.imageComp.common;

public class DiffResult {
    public boolean Success;
    public String StandardOut;
    public Double DifferenceAmount;

    public DiffResult(Boolean threwException, String output) {
        StandardOut = output;

        //IM can return non-zero codes for some comparisons, e.g. when the images are identical...
        //so fudge the "result" and just try to send a number back
        if(!threwException) {

            //TODO: is this a sensible result?
            if("1.#INF".equals(output)) {
                DifferenceAmount = 0D;
                Success = true;
            }
            else {
                //a bunch of metrics show additional breakdowns e.g. "1 (0.5, 0.5)", so just send the first number back
                int firstSpace = output.indexOf(" ");
                if(firstSpace != -1) {
                    output = output.substring(0, firstSpace);
                }

                try {
                    DifferenceAmount = Double.parseDouble(output);
                    Success = true;
                } catch (NumberFormatException e) { }
            }
        }
    }
}