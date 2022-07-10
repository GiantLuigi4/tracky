import com.tracky.MixinPlugin;

public class HashCodes {
	public static void main(String[] args) {
		System.out.println(MixinPlugin.class.toString());
		int code = MixinPlugin.class.toString().hashCode();
		
		String str = MixinPlugin.class.toString();
		for (int i = 0; i < 203993; i++) {
			String str1 = str.replace("com", "" + i);
			int code1 = str1.hashCode();
			if (code1 >code) {
				System.out.println(str1);
			}
		}
	}
}
