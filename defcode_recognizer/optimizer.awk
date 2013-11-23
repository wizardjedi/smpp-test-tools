BEGIN {
	start="";
	finish="";
	payload="";
}

{
	if ((start == "") && (finish == "") && (payload == "")) {
		start=$1;
		finish=$2;
		payload=$3;
	} else {

		newstart = $1;

		if ((payload == $3) && ((newstart-1) == finish)) {
			finish = $2;
		} else {
			print start ";" finish ";" payload

			start=$1;
        	        finish=$2;
                	payload=$3;
		}
	}
}

END {
	print start ";" finish ";" payload
}
