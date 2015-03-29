package gckfilestructure;

/**
 * Created by matyas on 04/03/15.
 */
public class Site {

    public Site() {
        this(true, true);
    }

    public Site(boolean hasName, boolean hasComment) {
        this(0, hasName, hasComment);
    }

    public Site(int position, boolean hasName, boolean hasComment) {
        super();
        this.position = position;
        this.hasComment = hasComment;
        this.hasName = hasName;
    }

    public void hasName(boolean hasName) {
        this.hasName = hasName;
    }

    public boolean hasName() {
        return hasName;
    }

    public void hasComment(boolean hasComment) {
        this.hasComment = hasComment;
    }

    public boolean hasComment() {
        return hasComment;
    }

    private boolean hasName;
    private boolean hasComment;
    private int position;
}
