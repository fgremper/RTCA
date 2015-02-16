SELECT main.branch AS branch, main.username AS username, main.commit AS commit, origin.commit AS origincommit FROM

(

	SELECT branchxuser.branch AS branch, branchxuser.username AS username, branches.commit AS commit FROM 

	(
		SELECT allbranches.branch AS branch, allusers.username FROM

		(
			SELECT DISTINCT branch FROM branches
			WHERE repositoryalias = ?
		) AS allbranches

		CROSS JOIN

		(
			SELECT DISTINCT username FROM useraccess
			WHERE repositoryalias = ?
			AND username <> "origin"
		) AS allusers

	) AS branchxuser

	LEFT OUTER JOIN 

	(
		SELECT branch, username, commit FROM branches
		WHERE repositoryalias = ?
	) AS branches

	ON branchxuser.branch = branches.branch
	AND branchxuser.username = branches.username

) AS main

LEFT OUTER JOIN

(
	SELECT branch, commit FROM branches
	WHERE repositoryalias = ?
	AND username = "origin"
) AS origin

ON origin.branch = main.branch
ORDER BY branch, username;

