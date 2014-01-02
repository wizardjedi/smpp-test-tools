BEGIN {
	operator=0;
	count=0;
}
{
	if ((operator == 0) && (count == 0)) {
		operator = $1
		count = $2
	}

	if (operator != $1) {
		print operator ";" count
		operator = $1
		count = $2
	} else {
		count=count+$2
	}
}
END {
	print operator ";" count
}
