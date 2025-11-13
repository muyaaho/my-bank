{{/*
Expand the name of the chart.
*/}}
{{- define "services.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "services.fullname" -}}
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
{{- define "services.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "services.labels" -}}
helm.sh/chart: {{ include "services.chart" . }}
{{ include "services.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: mybank
{{- end }}

{{/*
Selector labels for a specific service
*/}}
{{- define "services.selectorLabels" -}}
app.kubernetes.io/name: {{ include "services.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Service-specific selector labels
*/}}
{{- define "services.serviceSelectorLabels" -}}
app: {{ .name }}
tier: backend
{{- end }}

{{/*
Render environment variables from values
*/}}
{{- define "services.renderEnv" -}}
{{- range $key, $value := . }}
- name: {{ $key }}
  value: {{ tpl $value $ | quote }}
{{- end }}
{{- end }}
