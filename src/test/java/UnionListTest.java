import com.tracky.util.list.ObjectUnionList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;

public class UnionListTest {
	public static void main(String[] args) {
		ObjectUnionList<String> list = new ObjectUnionList<>(
				new ObjectArrayList<>(),
				Arrays.asList(
						"hi",
						"bye"
				),
				new ObjectArrayList<>(),
				new ObjectArrayList<>(Arrays.asList(
						"ih",
						"eyb"
				)),
				new ObjectArrayList<>()
		);
		list.add("eh");
		list.add("eyebv");
		
		for (int i = 0; i < list.size(); i++) {
			System.out.println(list.get(i));
		}
		
		System.out.println();
		
		for (String s : list) {
			System.out.println(s);
		}
	}
}
