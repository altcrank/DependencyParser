import java.util.LinkedList;
import java.util.Collections;

public class DependencyTree extends LinkedList<Arc> {

	private static final long serialVersionUID = 4099018308379880189L;
	
	public boolean isHead(int id) {
		for (Arc arc : this) {
			if (id == arc.getHead()) {
				return true;
			}
		}
		return false;
	}
	
	public void sort() {
		Collections.sort(this);
	}
}
