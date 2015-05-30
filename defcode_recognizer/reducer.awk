BEGIN {
	operator=0;
	delivered=0;
	sent=0;
}
{
	if ((operator == 0) && (sent == 0)) {
		operator = $1
		delivered = $2
		sent = $3
	}

	if (operator != $1) {
		print operator ";" delivered ";" sent
		operator = $1
		delivered = $2
		sent = $3
	} else {
		delivered=delivered+$2
		sent=sent+$3
	}
}
END {
	print operator ";" delivered ";" sent
}
