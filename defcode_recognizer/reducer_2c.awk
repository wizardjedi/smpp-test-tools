BEGIN {
	operator=0;
	sent=0;
}
{
	if ((operator == 0) && (sent == 0)) {
		operator = $1
		sent = $2
	} else {
		if (operator != $1) {
			print operator ";" sent
			operator = $1
			sent = $2
		} else {
			sent=sent+$2
		}
	}
}
END {
	print operator ";" sent
}
