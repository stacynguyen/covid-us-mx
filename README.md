## Comparing pandemic waves in US and MX (Spanish)

This report of May 2021 compares the COVID waves in the United States and Mexico since the beginning.
I wanted to see if my (informal) observation that the Mexican and American waves had been similar, and
to what extent. I did it at two levels: national, and NYC vs Mexico City.

I made an effort make the time series correspond in terms of date of death (which in turn correlate approximately
to date of contagion) and number of deaths (which depend on percentage of COVID deaths reported). The waves
turned out to be reasonably synchronized, with the noticeable exception on NYC's massive first wave, at
the beginning of the global epidemic.

Accurately computing deaths in turn required for the Mexican case a careful revision of official figures
to reach a more precise estimate than the official figures at the time (the Mexican authorities have been
changing their estimates as they go along, predictably revising down the deaths' estimate). So, an important
side result of this report is an actual, honest estimate of deaths due to COVID up to May 2022.

I also estimated the number of deaths that would have occurred in Mexico if the authorities here had been
as competent (or incompetent, if one prefers) in handling the epidemic as in the US. That number is more
conjectural, but we are still in the hundreds of thousands of extra deaths due to the incompetence of the
Mexican government.

[The report is here](Comparando%20Olas%20Pandemicas%20MX%20vs%20EEUU.pdf). 


## Code

Three interrelated Java programs, no packages involved. Main should be run `%java US_vs_MX 220507`.
Two csv files output to running directory. In all cases, paths in code should point to the correct
directories both for input and output.

There is also some lines of code in R to plot the tables.


## Data

The files involved in the calculus come from the Mexican registry and those from the state of New York.
They are contained in [the zipped directory](./zip.rar)
The only files not included is the COVID daily report from Mexico's Secretaría de Salud.
The file of 22 may 2022 (`220507COVID19MEXICO`, 2.4 GB) or after should give the same results. 

Also included: the csv files that are plotted in the report.


## Status

Report is finished.
