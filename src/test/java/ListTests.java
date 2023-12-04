import com.tracky.util.ObjectUnionList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;

public class ListTests {
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
		
		PositiveNegativeList<String> list1 = new PositiveNegativeList<>();
		list1.add("Test");
		list1.add("Test1");
		list1.add("Test2");
		list1.add("Test3");
		for (int i = 0; i < list1.size(); i++) {
			String v = list1.get(i);
			list1.setValue(i, v.equals("Test1") || v.equals("Test3"));
		}
		System.out.println();
		for (String s : list1.getPositive()) System.out.println(s);
		System.out.println();
		for (String s : list1.getNegative()) System.out.println(s);
	}
}
