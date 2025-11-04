{{/*
Expand the name of the chart.
*/}}
{{- define "mybank.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "mybank.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "mybank.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "mybank.labels" -}}
helm.sh/chart: {{ include "mybank.chart" . }}
{{ include "mybank.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.global.labels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "mybank.selectorLabels" -}}
app.kubernetes.io/name: {{ include "mybank.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "mybank.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "mybank.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Image pull policy
*/}}
{{- define "mybank.imagePullPolicy" -}}
{{- .Values.global.imagePullPolicy | default "IfNotPresent" }}
{{- end }}

{{/*
Full image name
*/}}
{{- define "mybank.image" -}}
{{- $registry := .registry | default $.Values.global.imageRegistry }}
{{- $repository := .repository }}
{{- $tag := .tag | default $.Chart.AppVersion }}
{{- printf "%s/%s:%s" $registry $repository $tag }}
{{- end }}
